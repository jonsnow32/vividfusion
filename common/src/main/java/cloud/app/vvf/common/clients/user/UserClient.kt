package cloud.app.vvf.common.clients.user

import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.user.User

interface UserClient {
    suspend fun loadUser(user: User): User
    fun getMediaItems(it: User): PagedData<MediaItemsContainer>
}
