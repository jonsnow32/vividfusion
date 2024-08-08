package cloud.app.common.clients

import cloud.app.common.helpers.PagedData
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.User

interface UserClient {
    suspend fun loadUser(user: User): User
    fun getMediaItems(it: User): PagedData<MediaItemsContainer>
}
