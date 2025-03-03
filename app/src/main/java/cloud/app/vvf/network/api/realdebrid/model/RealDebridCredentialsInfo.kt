package cloud.app.vvf.network.api.realdebrid.model

class RealDebridCredentialsInfo {
    var accessToken: String? = null
    var extensionId: String? = null
    var clientSecret: String? = null
    var refreshToken: String? = null
    val isValid: Boolean
        get() = !(accessToken == null || accessToken!!.isEmpty() || refreshToken == null || refreshToken!!.isEmpty() || extensionId == null || extensionId!!.isEmpty() || clientSecret == null || clientSecret!!.isEmpty())

    override fun toString(): String {
        return "RealDebridCredentialsInfo{accessToken='" + accessToken + '\'' + ", refreshToken='" + refreshToken + '\'' + ", extensionId='" + extensionId + '\'' + ", clientSecret='" + clientSecret + '\'' + '}'
    }
}
