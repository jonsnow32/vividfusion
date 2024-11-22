package cloud.app.vvf.network.api.realdebrid.model

data class RealDebridGetTokenResult(
    var access_token: String? = null,
    var expires_in: Long = 0,
    var refresh_token: String? = null,
    var token_type: String? = null,
    var last_clientID: String? = null,
    var last_clientSecret: String? = null,
)
