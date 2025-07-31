package cloud.app.vvf.network.api.torrentserver

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.Response
import java.net.URLEncoder

// Data classes copied from Torrent.kt (adjust package if needed)
data class TorrentRequest(
    val action: String,
    val hash: String = "",
    val link: String = "",
    val title: String = "",
    val poster: String = "",
    val data: String = "",
    val saveToDB: Boolean = false,
)
@Serializable
data class TorrentFileStat(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("path")
    val path: String? = null,
    @SerializedName("length")
    val length: Long? = null,
)
@Serializable
data class TorrentStatus(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("poster")
    val poster: String? = null,
    @SerializedName("data")
    val data: String? = null,
    @SerializedName("timestamp")
    val timestamp: Long? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("hash")
    val hash: String? = null,
    @SerializedName("stat")
    val stat: Int? = null,
    @SerializedName("stat_string")
    val statString: String? = null,
    @SerializedName("loaded_size")
    val loadedSize: Long? = null,
    @SerializedName("torrent_size")
    val torrentSize: Long? = null,
    @SerializedName("preloaded_bytes")
    val preloadedBytes: Long? = null,
    @SerializedName("preload_size")
    val preloadSize: Long? = null,
    @SerializedName("download_speed")
    val downloadSpeed: Double? = null,
    @SerializedName("upload_speed")
    val uploadSpeed: Double? = null,
    @SerializedName("total_peers")
    val totalPeers: Int? = null,
    @SerializedName("pending_peers")
    val pendingPeers: Int? = null,
    @SerializedName("active_peers")
    val activePeers: Int? = null,
    @SerializedName("connected_seeders")
    val connectedSeeders: Int? = null,
    @SerializedName("half_open_peers")
    val halfOpenPeers: Int? = null,
    @SerializedName("bytes_written")
    val bytesWritten: Long? = null,
    @SerializedName("bytes_written_data")
    val bytesWrittenData: Long? = null,
    @SerializedName("bytes_read")
    val bytesRead: Long? = null,
    @SerializedName("bytes_read_data")
    val bytesReadData: Long? = null,
    @SerializedName("bytes_read_useful_data")
    val bytesReadUsefulData: Long? = null,
    @SerializedName("chunks_written")
    val chunksWritten: Long? = null,
    @SerializedName("chunks_read")
    val chunksRead: Long? = null,
    @SerializedName("chunks_read_useful")
    val chunksReadUseful: Long? = null,
    @SerializedName("chunks_read_wasted")
    val chunksReadWasted: Long? = null,
    @SerializedName("pieces_dirtied_good")
    val piecesDirtiedGood: Long? = null,
    @SerializedName("pieces_dirtied_bad")
    val piecesDirtiedBad: Long? = null,
    @SerializedName("duration_seconds")
    val durationSeconds: Double? = null,
    @SerializedName("bit_rate")
    val bitRate: String? = null,
    @SerializedName("file_stats")
    val fileStats: List<TorrentFileStat>? = null,
    @SerializedName("trackers")
    val trackers: List<String>? = null,
) {
  fun streamUrl(torrentServerUrl: String, url: String): String {
    val fileName = this.fileStats?.firstOrNull { !it.path.isNullOrBlank() }?.path
      ?: throw Exception("Null path: fileStats=${this.fileStats}")

    val index = url.substringAfter("index=").substringBefore("&").toIntOrNull() ?: 0

    //  https://github.com/Diegopyl1209/torrentserver-aniyomi/blob/c18f58e51b6738f053261bc863177078aa9c1c98/web/api/stream.go#L18
    return "$torrentServerUrl/stream/${URLEncoder.encode(fileName, "utf-8")}?link=${this.hash}&index=$index&play"
  }
}

interface TorrentServerApi {
    @GET("/echo")
    suspend fun echo(): Response<ResponseBody>

    @GET("/shutdown")
    suspend fun shutdown(): Response<Unit>

    // List all torrents
    @POST("/torrents")
    suspend fun listTorrents(@Body request: TorrentRequest = TorrentRequest(action = "list")): Response<List<TorrentStatus>>

    // Add a torrent
    @POST("/torrents")
    suspend fun addTorrent(@Body request: TorrentRequest): Response<TorrentStatus>

    // Get torrent status by hash
    @POST("/torrents")
    suspend fun getTorrent(@Body request: TorrentRequest): Response<TorrentStatus>

    // Drop a torrent (close stream)
    @POST("/torrents")
    suspend fun dropTorrent(@Body request: TorrentRequest): Response<Unit>

    // Remove a torrent from server registry
    @POST("/torrents")
    suspend fun removeTorrent(@Body request: TorrentRequest): Response<Unit>
}
