package cloud.app.vvf.network.api.realdebrid.model

data class RealDebridGetDeviceCodeResult(
    val device_code: String,
    val user_code: String,
    val direct_verification_url: String? = null,
    val expires_in: Long = 0,
    val interval: Int = 0,
    val verification_url: String? = null
)
