package cloud.app.vvf.ui.common

import android.content.Context
import androidx.core.util.toKotlinPair
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.utils.UIHelper.resolveStyledDimension
import cloud.app.vvf.utils.toPx
import kotlin.math.floor
import kotlin.math.max

interface GridAdapter {
    val adapter: RecyclerView.Adapter<*>
    fun getSpanSize(position: Int, width: Int, count: Int): Int

    /**
     * Concat adapter that combines multiple GridAdapters and manages their span sizes
     */
    class Concat(
        vararg adapters: GridAdapter
    ) : GridAdapter {
        override val adapter = ConcatAdapter(adapters.map { it.adapter })
        private val getSpanSizeMap = adapters.mapIndexed { index, gridAdapter ->
            gridAdapter.adapter to gridAdapter::getSpanSize
        }.toMap()

        override fun getSpanSize(position: Int, width: Int, count: Int): Int {
            val (wrappedAdapter, pos) = adapter.getWrappedAdapterAndPosition(position).toKotlinPair()
            val getSpanSize = getSpanSizeMap[wrappedAdapter]
                ?: throw IllegalStateException("No span size function found for adapter: ${wrappedAdapter.javaClass.name}")
            return getSpanSize(pos, width, count)
        }
    }

    /**
     * Strategies for calculating span count
     */
    enum class SpanStrategy {
        /** Calculate based on item width, prefer even numbers */
        EVEN,
        /** Calculate based on item width, any number allowed */
        FLEXIBLE,
        /** Fixed span count */
        FIXED,
        /** Auto-detect from screen size with optimal spacing */
        AUTO
    }

    /**
     * Configuration for grid layout
     */
    data class GridConfig(
        val strategy: SpanStrategy = SpanStrategy.EVEN,
        val fixedSpanCount: Int = 2,
        val minSpanCount: Int = 1,
        val maxSpanCount: Int = Int.MAX_VALUE,
        val itemSpacing: Int = 8,
        val useStyledItemSize: Boolean = true,
        val itemWidthDp: Int? = null // Custom item width in DP, used when useStyledItemSize = false
    )

    companion object {
        /**
         * Configure RecyclerView with GridLayoutManager and custom config
         */
        fun configureGridLayout(
            recycler: RecyclerView,
            gridAdapter: GridAdapter,
            config: GridConfig = GridConfig()
        ) {
            val context = recycler.context
            val layoutManager = GridLayoutManager(context, 1)

            recycler.doOnLayout {
                val width = it.width - it.paddingLeft - it.paddingRight
                val spanCount = calculateSpanCount(context, width, config)

                layoutManager.spanCount = spanCount
                layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return try {
                            gridAdapter.getSpanSize(position, width, spanCount)
                        } catch (e: Exception) {
                            // Fallback to full span if error occurs
                            spanCount
                        }
                    }
                }
            }

            recycler.adapter = gridAdapter.adapter
            recycler.layoutManager = layoutManager
        }

        /**
         * Calculate optimal span count based on strategy
         */
        private fun calculateSpanCount(
            context: Context,
            availableWidth: Int,
            config: GridConfig
        ): Int {
            val spanCount = when (config.strategy) {
                SpanStrategy.FIXED -> config.fixedSpanCount

                SpanStrategy.EVEN, SpanStrategy.FLEXIBLE -> {
                    val itemWidth = when {
                        config.useStyledItemSize -> {
                            context.resolveStyledDimension(R.attr.itemCoverSize)
                        }
                        config.itemWidthDp != null -> {
                            // Convert DP to pixels
                            (config.itemWidthDp * context.resources.displayMetrics.density).toInt()
                        }
                        else -> {
                            // Fallback: use media_width from dimens
                            context.resources.getDimensionPixelSize(R.dimen.media_width)
                        }
                    }

                    val spacing = config.itemSpacing.toPx
                    val calc = if (itemWidth > 0) {
                        floor(availableWidth.toFloat() / (itemWidth + spacing)).toInt()
                    } else {
                        config.fixedSpanCount
                    }

                    // Apply EVEN strategy: prefer even numbers
                    if (config.strategy == SpanStrategy.EVEN && calc > 1) {
                        calc - (calc % 2)
                    } else {
                        max(1, calc)
                    }
                }

                SpanStrategy.AUTO -> {
                    // Auto-detect optimal span based on screen density and size
                    val displayMetrics = context.resources.displayMetrics
                    val widthDp = availableWidth / displayMetrics.density
                    when {
                        widthDp < 360 -> 2
                        widthDp < 600 -> 3
                        widthDp < 840 -> 4
                        else -> 6
                    }
                }
            }

            return spanCount.coerceIn(config.minSpanCount, config.maxSpanCount)
        }

        /**
         * Create a simple GridAdapter from a regular RecyclerView.Adapter
         * @param adapter The adapter to wrap
         * @param spanSize Function to determine span size for each item (default: full width)
         */
        fun fromAdapter(
            adapter: RecyclerView.Adapter<*>,
            spanSize: (position: Int, width: Int, count: Int) -> Int = { _, _, count -> count }
        ): GridAdapter {
            return object : GridAdapter {
                override val adapter: RecyclerView.Adapter<*> = adapter
                override fun getSpanSize(position: Int, width: Int, count: Int): Int {
                    return spanSize(position, width, count)
                }
            }
        }

        /**
         * Create a GridAdapter with uniform span size for all items
         * @param adapter The adapter to wrap
         * @param itemsPerRow Number of items per row
         */
        fun fromAdapter(
            adapter: RecyclerView.Adapter<*>,
            itemsPerRow: Int
        ): GridAdapter {
            return object : GridAdapter {
                override val adapter: RecyclerView.Adapter<*> = adapter
                override fun getSpanSize(position: Int, width: Int, count: Int): Int {
                    return count / itemsPerRow
                }
            }
        }
    }
}
