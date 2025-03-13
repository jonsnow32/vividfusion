package cloud.app.vvf.common.models

import cloud.app.vvf.common.helpers.PagedData

sealed class MediaItemsContainer {

  data class Category(
    val title: String,
    val subtitle: String? = null,
    val more: PagedData<AVPMediaItem>? = null,
  ) : MediaItemsContainer()

  data class Item(
    val media: AVPMediaItem
  ) : MediaItemsContainer()

  data class PageView(
    val title: String,
    val subtitle: String? = null,
    val items: List<AVPMediaItem>
  ) : MediaItemsContainer()

  fun sameAs(other: MediaItemsContainer) = when (this) {
    is Category -> other is Category && this.id == other.id
    is Item -> other is Item && media.sameAs(other.media)
    is PageView -> other is PageView && this.id == other.id
  }

  val id
    get() = when (this) {
      is Item -> media.id
      is Category -> title
      is PageView -> title
    }

  companion object {
    fun List<MediaItemsContainer>.toPaged() = PagedData.Single { this }
  }
}
