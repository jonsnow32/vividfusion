package cloud.app.common.models

import cloud.app.common.helpers.PagedData
import kotlinx.coroutines.flow.Flow

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
            else -> this.hashCode().toString()
        }
}
