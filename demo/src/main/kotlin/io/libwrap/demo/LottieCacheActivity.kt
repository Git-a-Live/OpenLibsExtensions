package io.libwrap.demo

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import io.libwrap.lottie.LottieCache

/**
 * 40 行同款动画：滚动时只有可见行在播放，回收时释放视图引用，
 * 所有行共享同一份 LottieComposition。
 */
class LottieCacheActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lottie)

        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@LottieCacheActivity)
            adapter = SpinnerAdapter(ITEM_COUNT)
        }
    }

    private class SpinnerAdapter(private val count: Int) :
        RecyclerView.Adapter<SpinnerAdapter.Holder>() {

        class Holder(val row: ViewGroup, val lottie: LottieAnimationView, val label: TextView) :
            RecyclerView.ViewHolder(row)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val row = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lottie, parent, false) as ViewGroup
            return Holder(
                row = row,
                lottie = row.findViewById(R.id.lottie),
                label = row.findViewById(R.id.label),
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.label.text = "第 ${position + 1} 行"
            LottieCache.play(holder.lottie, R.raw.spinner)
        }

        override fun onViewRecycled(holder: Holder) {
            LottieCache.release(holder.lottie)
        }

        override fun getItemCount(): Int = count
    }

    private companion object {
        const val ITEM_COUNT = 40
    }
}
