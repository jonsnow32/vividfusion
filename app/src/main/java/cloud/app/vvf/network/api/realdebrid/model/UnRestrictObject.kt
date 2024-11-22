package cloud.app.vvf.network.api.realdebrid.model

class UnRestrictObject {
    /**
     * id : string
     * filename : string
     * filesize : long
     * link : string
     * host : string
     * chunks : int
     * crc : int
     * download : string
     * streamable : int
     * type : string
     * alternative : [{"id":"string","filename":"string","download":"string","type":"string"},{"id":"string","filename":"string","download":"string","type":"string"}]
     */
    var id: String? = null
    var filename: String? = null
    var filesize: Long = 0
    var link: String? = null
    var host: String? = null
    var chunks: String? = null
    var crc: String? = null
    var download: String? = null
    var streamable: String? = null
    var type: String? = null
    var alternative: List<AlternativeBean>? = null

    class AlternativeBean {
        /**
         * id : string
         * filename : string
         * download : string
         * type : string
         */
        var id: String? = null
        var filename: String? = null
        var download: String? = null
        var type: String? = null
    }


}
