package cloud.app.avp.network.api.openSubtitle.models

data class LoginResponse(
    val base_url: String,
    val status: Int,
    val token: String,
    val user: User
)
