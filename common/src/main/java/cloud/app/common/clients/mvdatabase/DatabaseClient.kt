package cloud.app.common.clients.mvdatabase

import cloud.app.common.clients.BaseClient
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.QuickSearchItem
import cloud.app.common.models.Tab

interface DatabaseClient : BaseClient{
  suspend fun getHomeTabs(): List<Tab>
  fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer>
  suspend fun getMediaDetail(avpMediaItem: AVPMediaItem): AVPMediaItem?
  fun getKnowFor(actor: AVPMediaItem.ActorItem): PagedData<AVPMediaItem>
  fun getRecommended(avpMediaItem: AVPMediaItem): PagedData<AVPMediaItem>
  suspend fun quickSearch(query: String): List<QuickSearchItem>
  suspend fun searchTabs(query: String?): List<Tab>
  fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer>
}

