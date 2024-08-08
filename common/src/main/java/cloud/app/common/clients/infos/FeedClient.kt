package cloud.app.common.clients.infos

import cloud.app.common.helpers.PagedData
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.Tab

interface FeedClient {
    suspend fun getHomeTabs(): List<Tab>
    fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer>
}

