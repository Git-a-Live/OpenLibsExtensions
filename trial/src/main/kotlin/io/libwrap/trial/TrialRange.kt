package io.libwrap.trial

/**
 * 试听区间，用比例描述，与时长单位无关。
 *
 * 比例而非毫秒：控件不感知媒体时长，宿主换算成比例后传入，避免控件内部再做一次
 * 「毫秒 ÷ 时长」的换算而产生两套坐标系。
 */
public data class TrialRange(public val start: Float, public val end: Float) {

    init {
        require(start in 0f..1f) { "start 必须在 [0, 1] 内，当前为 $start" }
        require(end in 0f..1f) { "end 必须在 [0, 1] 内，当前为 $end" }
        require(start < end) { "start 必须小于 end，当前为 $start..$end" }
    }

    public val length: Float
        get() = end - start

    public operator fun contains(fraction: Float): Boolean = fraction in start..end

    public companion object {

        /** 覆盖整条轨道。 */
        public val Full: TrialRange = TrialRange(0f, 1f)

        /**
         * 由毫秒换算为比例；时长非正时返回 null。
         *
         * 起止点会被钳制到 `[0, duration]`，换算后若区间长度为零则返回 null。
         */
        public fun ofMillis(
            startMillis: Long,
            endMillis: Long,
            durationMillis: Long,
        ): TrialRange? {
            if (durationMillis <= 0L) return null
            val start = startMillis.coerceIn(0L, durationMillis).toFloat() / durationMillis
            val end = endMillis.coerceIn(0L, durationMillis).toFloat() / durationMillis
            return if (start < end) TrialRange(start, end) else null
        }
    }
}

/**
 * 端点样式。
 *
 * [Custom] 供宿主自行绘制；其余为内置形状，按像素尺寸与颜色描述，
 * 不依赖任何主题资源。
 */
public sealed interface TrialEndpoint {

    public data object None : TrialEndpoint

    /** 竖向短线，宽度为 [widthPx]，贯穿轨道高度。 */
    public data class Line(public val color: Int, public val widthPx: Float) : TrialEndpoint

    /** 圆点，半径为 [radiusPx]，圆心位于轨道垂直中央。 */
    public data class Circle(public val color: Int, public val radiusPx: Float) : TrialEndpoint

    /** 自定义绘制；[draw] 收到的坐标以轨道中心线为 `y`。 */
    public class Custom(public val draw: EndpointDrawer) : TrialEndpoint
}

/** 自定义端点绘制入口。坐标系原点为控件左上角，[centerY] 为轨道垂直中心。 */
public fun interface EndpointDrawer {
    public fun draw(canvas: android.graphics.Canvas, x: Float, centerY: Float, trackHeightPx: Float)
}

/**
 * 控件外观。全部以像素与颜色值描述，不读取主题或资源。
 *
 * @param trackColor 区间外的轨道底色
 * @param progressColor 区间外的已播放颜色
 * @param trialTrackColor 区间内的轨道底色
 * @param trialProgressColor 区间内的已播放颜色
 * @param trackHeightPx 轨道高度
 * @param cornerRadiusPx 轨道圆角半径，`null` 表示胶囊形
 * @param startEndpoint 区间起点端点
 * @param endEndpoint 区间终点端点
 */
public data class TrialSeekBarStyle(
    public val trackColor: Int,
    public val progressColor: Int,
    public val trialTrackColor: Int,
    public val trialProgressColor: Int,
    public val trackHeightPx: Float,
    public val cornerRadiusPx: Float? = null,
    public val startEndpoint: TrialEndpoint = TrialEndpoint.None,
    public val endEndpoint: TrialEndpoint = TrialEndpoint.None,
)
