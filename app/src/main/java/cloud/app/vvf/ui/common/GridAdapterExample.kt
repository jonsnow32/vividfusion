//package cloud.app.vvf.ui.common
//
//import androidx.recyclerview.widget.RecyclerView
//
///**
// * Ví dụ sử dụng GridAdapter - Universal Adapter Container
// *
// * GridAdapter có thể chứa tất cả các adapter khác và tự động tính spanCount
// * cho GridLayoutManager
// */
//object GridAdapterExample {
//
//    /**
//     * Ví dụ 1: Kết hợp nhiều adapter với Concat
//     */
//    fun example1ConcatMultipleAdapters(
//        headerAdapter: RecyclerView.Adapter<*>,
//        movieAdapter: RecyclerView.Adapter<*>,
//        footerAdapter: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        // Wrap các adapter thành GridAdapter
//        val headerGrid = GridAdapter.fromAdapter(headerAdapter) { _, _, count ->
//            count // Header chiếm full width
//        }
//
//        val movieGrid = GridAdapter.fromAdapter(movieAdapter, itemsPerRow = 3) // 3 items/row
//
//        val footerGrid = GridAdapter.fromAdapter(footerAdapter) { _, _, count ->
//            count // Footer chiếm full width
//        }
//
//        // Kết hợp tất cả với Concat
//        val combinedAdapter = GridAdapter.Concat(headerGrid, movieGrid, footerGrid)
//
//        // Configure RecyclerView với auto span calculation
//        GridAdapter.configureGridLayout(recyclerView, combinedAdapter)
//    }
//
//    /**
//     * Ví dụ 2: Sử dụng SpanStrategy.EVEN (số chẵn)
//     */
//    fun example2EvenStrategy(
//        adapter: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        val gridAdapter = GridAdapter.fromAdapter(adapter, itemsPerRow = 2)
//
//        val config = GridAdapter.GridConfig(
//            strategy = GridAdapter.SpanStrategy.EVEN,
//            itemSpacing = 16
//        )
//
//        GridAdapter.configureGridLayout(recyclerView, gridAdapter, config)
//    }
//
//    /**
//     * Ví dụ 3: Sử dụng SpanStrategy.FLEXIBLE (linh hoạt)
//     */
//    fun example3FlexibleStrategy(
//        adapter: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        val gridAdapter = GridAdapter.fromAdapter(adapter, itemsPerRow = 3)
//
//        val config = GridAdapter.GridConfig(
//            strategy = GridAdapter.SpanStrategy.FLEXIBLE,
//            minSpanCount = 2,
//            maxSpanCount = 6
//        )
//
//        GridAdapter.configureGridLayout(recyclerView, gridAdapter, config)
//    }
//
//    /**
//     * Ví dụ 4: Sử dụng SpanStrategy.FIXED (cố định)
//     */
//    fun example4FixedStrategy(
//        adapter: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        val gridAdapter = GridAdapter.fromAdapter(adapter, itemsPerRow = 4)
//
//        val config = GridAdapter.GridConfig(
//            strategy = GridAdapter.SpanStrategy.FIXED,
//            fixedSpanCount = 4
//        )
//
//        GridAdapter.configureGridLayout(recyclerView, gridAdapter, config)
//    }
//
//    /**
//     * Ví dụ 5: Sử dụng SpanStrategy.AUTO (tự động theo màn hình)
//     */
//    fun example5AutoStrategy(
//        adapter: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        val gridAdapter = GridAdapter.fromAdapter(adapter) { _, _, count ->
//            count / 2 // Mỗi item chiếm 1/2 width
//        }
//
//        val config = GridAdapter.GridConfig(
//            strategy = GridAdapter.SpanStrategy.AUTO
//        )
//
//        GridAdapter.configureGridLayout(recyclerView, gridAdapter, config)
//    }
//
//    /**
//     * Ví dụ 6: Custom span size cho từng item (advanced)
//     */
//    fun example6CustomSpanSize(
//        adapter: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        val gridAdapter = GridAdapter.fromAdapter(adapter) { position, width, count ->
//            when {
//                position % 5 == 0 -> count // Item đầu tiên mỗi 5 item chiếm full width
//                position % 3 == 0 -> count / 2 // Mỗi item thứ 3 chiếm 1/2 width
//                else -> count / 3 // Các item còn lại chiếm 1/3 width
//            }
//        }
//
//        GridAdapter.configureGridLayout(recyclerView, gridAdapter)
//    }
//
//    /**
//     * Ví dụ 7: Kết hợp nhiều adapter với span size khác nhau
//     */
//    fun example7ComplexGrid(
//        bannerAdapter: RecyclerView.Adapter<*>,
//        categoryAdapter: RecyclerView.Adapter<*>,
//        productAdapter: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        // Banner - full width
//        val bannerGrid = GridAdapter.fromAdapter(bannerAdapter) { _, _, count -> count }
//
//        // Category - 4 items per row
//        val categoryGrid = GridAdapter.fromAdapter(categoryAdapter, itemsPerRow = 4)
//
//        // Product - custom logic
//        val productGrid = GridAdapter.fromAdapter(productAdapter) { position, _, count ->
//            if (position == 0) count // Featured product - full width
//            else count / 2 // Normal products - 2 per row
//        }
//
//        // Kết hợp tất cả
//        val combined = GridAdapter.Concat(bannerGrid, categoryGrid, productGrid)
//
//        val config = GridAdapter.GridConfig(
//            strategy = GridAdapter.SpanStrategy.FLEXIBLE,
//            itemSpacing = 12,
//            minSpanCount = 2,
//            maxSpanCount = 8
//        )
//
//        GridAdapter.configureGridLayout(recyclerView, combined, config)
//    }
//
//    /**
//     * Ví dụ 8: Nested Concat (lồng nhau)
//     */
//    fun example8NestedConcat(
//        header: RecyclerView.Adapter<*>,
//        section1: RecyclerView.Adapter<*>,
//        section2: RecyclerView.Adapter<*>,
//        footer: RecyclerView.Adapter<*>,
//        recyclerView: RecyclerView
//    ) {
//        val headerGrid = GridAdapter.fromAdapter(header) { _, _, count -> count }
//
//        // Kết hợp 2 sections
//        val sectionsGrid = GridAdapter.Concat(
//            GridAdapter.fromAdapter(section1, itemsPerRow = 2),
//            GridAdapter.fromAdapter(section2, itemsPerRow = 3)
//        )
//
//        val footerGrid = GridAdapter.fromAdapter(footer) { _, _, count -> count }
//
//        // Kết hợp tất cả
//        val finalGrid = GridAdapter.Concat(headerGrid, sectionsGrid, footerGrid)
//
//        GridAdapter.configureGridLayout(recyclerView, finalGrid)
//    }
//}
//
