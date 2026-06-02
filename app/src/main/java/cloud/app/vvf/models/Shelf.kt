package cloud.app.vvf.models

sealed class Shelf(open val title: String? = null) {
  data class Items(
    override val title: String?,
    val data: List<AppItem>
  ) : Shelf(title)

  data class Categories(
    override val title: String?,
    val data: List<Category>
  ) : Shelf(title)
}
