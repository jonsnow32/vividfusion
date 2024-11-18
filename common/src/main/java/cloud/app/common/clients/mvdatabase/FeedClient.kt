package cloud.app.common.clients.mvdatabase

import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.Tab

interface FeedClient {
    suspend fun getHomeTabs(): List<Tab>
    fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer>
    suspend fun getMediaDetail(avpMediaItem: AVPMediaItem): AVPMediaItem?
    fun getKnowFor(actor: AVPMediaItem.ActorItem): PagedData<AVPMediaItem>
}

