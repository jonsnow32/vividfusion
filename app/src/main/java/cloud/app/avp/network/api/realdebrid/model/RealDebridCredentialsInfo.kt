package cloud.app.avp.network.api.realdebrid.model

class RealDebridCredentialsInfo {
    var accessToken: String? = null
    var clientId: String? = null
    var clientSecret: String? = null
    var refreshToken: String? = null
    val isValid: Boolean
        get() = !(accessToken == null || accessToken!!.isEmpty() || refreshToken == null || refreshToken!!.isEmpty() || clientId == null || clientId!!.isEmpty() || clientSecret == null || clientSecret!!.isEmpty())

    override fun toString(): String {
        return "RealDebridCredentialsInfo{accessToken='" + accessToken + '\'' + ", refreshToken='" + refreshToken + '\'' + ", clientId='" + clientId + '\'' + ", clientSecret='" + clientSecret + '\'' + '}'
    }
}
