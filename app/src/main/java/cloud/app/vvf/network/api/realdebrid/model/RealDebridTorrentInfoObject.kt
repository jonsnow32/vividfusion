package cloud.app.vvf.network.api.realdebrid.model


import kotlinx.serialization.Serializable

@Serializable
class RealDebridTorrentInfoObject  {
    /**
     * id : HCCVEDF6ZKPKC
     * filename : rep-avengersinfinitywar.2018.1080p.bluray.x264[EtHD].mkv
     * original_filename : Avengers.Infinity.War.2018.1080p.BluRay.x264-Replica[EtHD]
     * hash : ad85c201325b75324e8627346645bc1169b0a1fc
     * bytes : 12902318788
     * original_bytes : 13034576071
     * host : real-debrid.com
     * split : 2000
     * progress : 100
     * status : downloaded
     * added : 2019-02-25T01:53:04.000Z
     * files : [{"id":1,"path":"/Downloaded from www.ETTV.tv .txt","bytes":270,"selected":0},{"id":2,"path":"/Proof/rep-avengersinfinitywar.2018.1080p.bluray.x264-proof.jpg","bytes":36410,"selected":0},{"id":3,"path":"/Sample/rep-avengersinfinitywar.2018.1080p.bluray.x264-sample.mkv","bytes":91810137,"selected":0},{"id":4,"path":"/Subs/rep-avengersinfinitywar.2018.1080p.bluray.x264-subs.sfv","bytes":66,"selected":0},{"id":5,"path":"/Subs/rep-avengersinfinitywar.2018.1080p.bluray.x264.idx","bytes":307896,"selected":0},{"id":6,"path":"/Subs/rep-avengersinfinitywar.2018.1080p.bluray.x264.sub","bytes":39794688,"selected":0},{"id":7,"path":"/rep-avengersinfinitywar.2018.1080p.bluray.x264.jpg","bytes":295869,"selected":0},{"id":8,"path":"/rep-avengersinfinitywar.2018.1080p.bluray.x264.nfo","bytes":6640,"selected":0},{"id":9,"path":"/rep-avengersinfinitywar.2018.1080p.bluray.x264.sfv","bytes":5307,"selected":0},{"id":10,"path":"/rep-avengersinfinitywar.2018.1080p.bluray.x264[EtHD].mkv","bytes":12902318788,"selected":1}]
     * links : ["https://real-debrid.com/d/SANLCK7BI34XE"]
     * ended : 2018-08-01T09:54:22.000Z
     */
    var id: String? = null
    var filename: String? = null
    var original_filename: String? = null
    var hash: String? = null
    var bytes: Long = 0
    var original_bytes: Long = 0
    var host: String? = null
    var split = 0
    var progress = 0.0f
    var status: String? = null
    var added: String? = null
    var ended: String? = null
    var seeders = 0
    var files: List<FilesBean>? = null
    var links: List<String>? = null
    var speed: Long = 0
    var isGotDetails = false


    val fileIDList: List<String>
        get() {
            val fileIDs = mutableListOf<String>()
            files?.let {
                for (filesBean in it) fileIDs.add(filesBean.id.toString())
            }
            return fileIDs
        }

    @Serializable
    class FilesBean  {
        /**
         * id : 1
         * path : /Downloaded from www.ETTV.tv .txt
         * bytes : 270
         * selected : 0
         */
        var id = 0
        var path: String? = null
        var bytes: Long = 0
        var selected = 0
        var link: String? = null


        override fun toString(): String {
            return id.toString()
        }

    }

    override fun toString(): String {
        return "RealDebridTorrentInfoObject(id=$id, filename=$filename, original_filename=$original_filename, hash=$hash, bytes=$bytes, original_bytes=$original_bytes, host=$host, split=$split, progress=$progress, status=$status, added=$added, ended=$ended, seeders=$seeders, files=$files, links=$links, speed=$speed, isGotDetails=$isGotDetails, fileIDList=$fileIDList)"
    }


}
