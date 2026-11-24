package io.libwrap.trial

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 展示试听区间的进度条。
 *
 * 进度以 **0..1 的比例**描述，控件不持有媒体时长：宿主将毫秒位置换算为比例后写入，
 * 控件内部只维护一套坐标系。
 *
 * 区间内外的底色与已播放颜色可分别配置，试听区间端点支持内置形状与自定义绘制。
 * 拖动越出区间时通过 [OnTrialSeekBarChangeListener.onSeekOutOfRange] 通知宿主，
 * 是否把进度钳制在区间内由 [clampToRange] 决定。
 */
public class TrialSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** 交互回调。比例取值均在 `0..1`。 */
    public interface OnTrialSeekBarChangeListener {

        public fun onStartTracking()

        /** @param fromUser true 表示由拖动或按键触发，false 表示由 [setProgress] 写入 */
        public fun onProgressChanged(progress: Float, fromUser: Boolean)

        public fun onStopTracking(progress: Float)

        /**
         * 拖动越出试听区间。
         *
         * @param requested 手指对应的原始比例，可能小于 0 或大于 1
         */
        public fun onSeekOutOfRange(requested: Float)
    }

    /** 轨道外观。赋值后立即重绘。 */
    public var style: TrialSeekBarStyle = TrialSeekBarStyle(
        trackColor = 0x33FFFFFF,
        progressColor = 0xFFFFFFFF.toInt(),
        trialTrackColor = 0x33FFFFFF,
        trialProgressColor = 0xFFFFC107.toInt(),
        trackHeightPx = 6f * resources.displayMetrics.density,
    )
        set(value) {
            field = value
            invalidate()
        }

    /** 试听区间；null 表示不展示区间，整条轨道按可用范围处理。 */
    public var trialRange: TrialRange? = null
        set(value) {
            field = value
            val clamped = clamp(field, progress)
            if (clamped != progress) {
                progress = clamped
            }
            invalidate()
        }

    /** 当前进度，取值 `0..1`。 */
    public var progress: Float = 0f
        private set

    /** 拖动或按键越出区间时是否把进度钳制在区间内。 */
    public var clampToRange: Boolean = true

    /** 按键调整的步长。 */
    public var keyStep: Float = 0.01f

    public var listener: OnTrialSeekBarChangeListener? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val endpointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackRect = RectF()

    private var tracking = false

    init {
        isClickable = true
        isFocusable = true
    }

    /**
     * 写入进度。
     *
     * 拖动进行中时忽略来自播放器的写入，避免旧位置把手指位置顶回去；拖动结束后恢复。
     *
     * @param fromUser 由交互触发时传 true，播放器回写传 false
     */
    public fun setProgress(progress: Float, fromUser: Boolean = false) {
        if (tracking && !fromUser) return
        val target = clamp(trialRange, progress.coerceIn(0f, 1f))
        if (target == this.progress) return
        this.progress = target
        listener?.onProgressChanged(target, fromUser)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (style.trackHeightPx + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(maxOf(desiredHeight, suggestedMinimumHeight), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        if (right <= left) return
        val centerY = (height / 2).toFloat()
        val halfHeight = style.trackHeightPx / 2f
        val top = centerY - halfHeight
        val bottom = centerY + halfHeight
        val radius = style.cornerRadiusPx ?: halfHeight

        trackRect.set(left, top, right, bottom)

        trackPaint.color = style.trackColor
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        val range = trialRange
        if (range != null) {
            trackPaint.color = style.trialTrackColor
            val segLeft = left + range.start * (right - left)
            val segRight = left + range.end * (right - left)
            canvas.drawRoundRect(segLeft, top, segRight, bottom, radius, radius, trackPaint)
        }

        val fillStart = range?.start ?: 0f
        if (progress > fillStart) {
            progressPaint.color = if (range != null) style.trialProgressColor else style.progressColor
            val fillLeft = left + fillStart * (right - left)
            val fillRight = left + progress * (right - left)
            val save = canvas.save()
            canvas.clipRect(fillLeft, top, fillRight, bottom)
            canvas.drawRoundRect(trackRect, radius, radius, progressPaint)
            canvas.restoreToCount(save)
        }

        if (range != null) {
            drawEndpoint(canvas, style.startEndpoint, left + range.start * (right - left), centerY)
            drawEndpoint(canvas, style.endEndpoint, left + range.end * (right - left), centerY)
        }
    }

    private fun drawEndpoint(canvas: Canvas, endpoint: TrialEndpoint, x: Float, centerY: Float) {
        when (endpoint) {
            TrialEndpoint.None -> Unit
            is TrialEndpoint.Line -> {
                endpointPaint.color = endpoint.color
                val halfWidth = endpoint.widthPx / 2f
                val halfHeight = style.trackHeightPx / 2f
                canvas.drawRect(x - halfWidth, centerY - halfHeight, x + halfWidth, centerY + halfHeight, endpointPaint)
            }

            is TrialEndpoint.Circle -> {
                endpointPaint.color = endpoint.color
                canvas.drawCircle(x, centerY, endpoint.radiusPx, endpointPaint)
            }

            is TrialEndpoint.Custom -> endpoint.draw.draw(canvas, x, centerY, style.trackHeightPx)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = true
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onStartTracking()
                updateFromTouch(event.x, fromUser = true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (tracking) {
                    updateFromTouch(event.x, fromUser = true)
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (tracking) {
                    tracking = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    listener?.onStopTracking(progress)
                    performClick()
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                if (tracking) {
                    tracking = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val delta = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS -> -keyStep
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PLUS -> keyStep
            else -> return super.onKeyDown(keyCode, event)
        }
        val requested = (progress + delta).coerceIn(0f, 1f)
        val range = trialRange
        if (range != null && requested !in range) {
            listener?.onSeekOutOfRange(requested)
        }
        setProgress(if (clampToRange) clamp(range, requested) else requested, fromUser = true)
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.SeekBar"
        info.rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
            0f,
            1f,
            progress,
        )
    }

    private fun updateFromTouch(x: Float, fromUser: Boolean) {
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        if (right <= left) return
        val requested = ((x - left) / (right - left)).coerceIn(0f, 1f)
        val range = trialRange
        if (range != null && requested !in range) {
            listener?.onSeekOutOfRange(requested)
        }
        setProgress(if (clampToRange) clamp(range, requested) else requested, fromUser)
    }

    private fun clamp(range: TrialRange?, value: Float): Float {
        if (range == null) return value.coerceIn(0f, 1f)
        return if (clampToRange) value.coerceIn(range.start, range.end) else value.coerceIn(0f, 1f)
    }
}
