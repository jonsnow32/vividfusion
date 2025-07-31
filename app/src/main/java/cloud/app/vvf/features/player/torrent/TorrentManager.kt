package cloud.app.vvf.features.player.torrent

import android.content.Context
import cloud.app.vvf.network.api.torrentserver.TorrentRequest
import cloud.app.vvf.network.api.torrentserver.TorrentServerApi
import cloud.app.vvf.network.api.torrentserver.TorrentStatus
import cloud.app.vvf.network.di.TorrentServerApiFactory
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import torrServer.TorrServer

@Singleton
class TorrentManager @Inject constructor(private val apiFactory: TorrentServerApiFactory  ) {
  private var serverPort = 0L;
  private val api: TorrentServerApi by lazy {
    if (serverPort <= 0) {
      throw IllegalStateException("Torrent server is not running or port is not set")
    }
    apiFactory.create(serverPort)
  }

  suspend fun echo(): Boolean = withContext(Dispatchers.IO) {
    try {
      val response = api.echo()
      response.body()?.string()?.isNotEmpty() == true
    } catch (e: Exception) {
      Timber.e(e, "Failed to echo torrent server")
      false
    }
  }

  suspend fun shutdown(): Boolean = withContext(Dispatchers.IO) {
    try {
      api.shutdown().isSuccessful
    } catch (e: Exception) {
      false
    }
  }

  suspend fun list(): List<TorrentStatus> = withContext(Dispatchers.IO) {
    val response = api.listTorrents(TorrentRequest(action = "list"))
    response.body() ?: emptyList()
  }

  private suspend fun drop(hash: String): Boolean = withContext(Dispatchers.IO) {
    try {
      api.dropTorrent(TorrentRequest(action = "drop", hash = hash)).isSuccessful
    } catch (e: Exception) {
      false
    }
  }

  suspend fun rem(hash: String): Boolean = withContext(Dispatchers.IO) {
    try {
      api.removeTorrent(TorrentRequest(action = "rem", hash = hash)).isSuccessful
    } catch (e: Exception) {
      false
    }
  }

  suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
    try {
      val items = list()
      var allSuccess = true
      for (item in items) {
        val hash = item.hash ?: continue
        if (!drop(hash)) allSuccess = false
        if (!rem(hash)) allSuccess = false
      }
      allSuccess
    } catch (e: Exception) {
      false
    }
  }

  suspend fun get(hash: String): TorrentStatus? = withContext(Dispatchers.IO) {
    val response = api.getTorrent(TorrentRequest(action = "get", hash = hash))
    response.body()
  }

  suspend fun add(url: String): TorrentStatus? = withContext(Dispatchers.IO) {
    val response = api.addTorrent(TorrentRequest(action = "add", link = url))
    response.body()
  }

  private suspend fun setup(dir: String): Boolean {
    go.Seq.load()
    if (echo()) {
      return true
    }
    serverPort = TorrServer.startTorrentServer(dir, 0)
    if (serverPort < 0) {
      return false
    }
    TorrServer.addTrackers(trackers.joinToString(separator = ",\n"))
    return echo()
  }

  private val trackers = listOf(
    "udp://tracker.opentrackr.org:1337/announce",
    "https://tracker2.ctix.cn/announce",
    "https://tracker1.520.jp:443/announce",
    "udp://opentracker.i2p.rocks:6969/announce",
    "udp://open.tracker.cl:1337/announce",
    "udp://open.demonii.com:1337/announce",
    "http://tracker.openbittorrent.com:80/announce",
    "udp://tracker.openbittorrent.com:6969/announce",
    "udp://open.stealth.si:80/announce",
    "udp://exodus.desync.com:6969/announce",
    "udp://tracker-udp.gbitt.info:80/announce",
    "udp://explodie.org:6969/announce",
    "https://tracker.gbitt.info:443/announce",
    "http://tracker.gbitt.info:80/announce",
    "udp://uploads.gamecoast.net:6969/announce",
    "udp://tracker1.bt.moack.co.kr:80/announce",
    "udp://tracker.tiny-vps.com:6969/announce",
    "udp://tracker.theoks.net:6969/announce",
    "udp://tracker.dump.cl:6969/announce",
    "udp://tracker.bittor.pw:1337/announce",
    "https://tracker1.520.jp:443/announce",
    "udp://opentracker.i2p.rocks:6969/announce",
    "udp://open.tracker.cl:1337/announce",
    "udp://open.demonii.com:1337/announce",
    "http://tracker.openbittorrent.com:80/announce",
    "udp://tracker.openbittorrent.com:6969/announce",
    "udp://open.stealth.si:80/announce",
    "udp://exodus.desync.com:6969/announce",
    "udp://tracker-udp.gbitt.info:80/announce",
    "udp://explodie.org:6969/announce",
    "https://tracker.gbitt.info:443/announce",
    "http://tracker.gbitt.info:80/announce",
    "udp://uploads.gamecoast.net:6969/announce",
    "udp://tracker1.bt.moack.co.kr:80/announce",
    "udp://tracker.tiny-vps.com:6969/announce",
    "udp://tracker.theoks.net:6969/announce",
    "udp://tracker.dump.cl:6969/announce",
    "udp://tracker.bittor.pw:1337/announce"
  )

  // Add more methods as needed, e.g., transformLink, setup, etc.
  suspend fun transformLink(link: String, cacheDir: File): Pair<String, TorrentStatus> = withContext(Dispatchers.IO) {
    val defaultDirectory = File(cacheDir, "torrent_tmp")
    defaultDirectory.mkdirs()
    if (!setup(defaultDirectory.absolutePath)) {
        throw Exception("Unable to setup the torrent server")
    }
    val status = add(link) ?: throw Exception("Failed to add torrent")
    status.streamUrl("http://127.0.0.1:${serverPort}", link) to status
  }
  fun deleteAllFiles(context: Context) {
    //todo
  }

  companion object {
    var hasAcceptedTorrentForThisSession: Boolean = false
  }
}
