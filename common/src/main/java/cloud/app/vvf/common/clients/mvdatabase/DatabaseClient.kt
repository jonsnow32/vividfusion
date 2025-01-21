package cloud.app.vvf.common.clients.mvdatabase

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.Tab

interface DatabaseClient : BaseClient{
  suspend fun getHomeTabs(): List<Tab>
  fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer>
  suspend fun getMediaDetail(avpMediaItem: AVPMediaItem): AVPMediaItem?
  fun getKnowFor(actor: AVPMediaItem.ActorItem): PagedData<AVPMediaItem>
  fun getRecommended(avpMediaItem: AVPMediaItem): PagedData<AVPMediaItem>
  suspend fun quickSearch(query: String): List<SearchItem>
  suspend fun searchTabs(query: String?): List<Tab>
  fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer>
}

