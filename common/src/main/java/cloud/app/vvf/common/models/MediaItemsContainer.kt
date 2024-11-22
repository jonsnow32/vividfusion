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

    fun sameAs(other: MediaItemsContainer) = when (this) {
        is Category -> other is Category && this.id == other.id
        is Item -> other is Item && media.sameAs(other.media)
    }

    val id
        get() = when (this) {
            is Item -> media.id
            is Category -> title
        }
}
