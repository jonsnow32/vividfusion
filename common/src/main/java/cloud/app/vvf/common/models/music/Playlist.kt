package cloud.app.vvf.common.models.music

import cloud.app.vvf.common.models.ImageHolder
import cloud.app.vvf.common.models.user.User
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val isEditable: Boolean,
    val cover: ImageHolder? = null,
    val authors: List<User> = listOf(),
    val creationDate: String? = null,
    val duration: Long? = null,
    val description: String? = null,
    val subtitle: String? = null,
    val tracks: List<Track>? = null,
    val extras: Map<String, String> = mapOf()
)
