package cloud.app.common.clients.mvdatabase

import cloud.app.common.helpers.PagedData
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.QuickSearchItem
import cloud.app.common.models.Tab

interface SearchClient {
  suspend fun quickSearch(query: String): List<QuickSearchItem>
  suspend fun searchTabs(query: String?): List<Tab>
  fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer>
}
