package cloud.app.avp.network.api.realdebrid.model


import kotlinx.serialization.Serializable

@Serializable
class UnRestrictCheckObject  {
    /**
     * host : string
     * link : string
     * filename : string
     * filesize : 0
     * supported : 0
     */
    var host: String? = null
    var link: String? = null
    var host_icon: String? = null
    var filename: String? = null
    var filesize: Long = 0
    var supported = 0
}
