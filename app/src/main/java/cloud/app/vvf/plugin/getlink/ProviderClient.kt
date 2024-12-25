package cloud.app.vvf.plugin.getlink

import androidx.annotation.Discouraged
import cloud.app.vvf.extension.provider.M4UFree
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.SubtitleData
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting
import cloud.app.vvf.common.settings.SettingSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher

class ProviderClient: StreamClient {
    override val defaultSettings: List<Setting> = listOf(
        SettingSwitch(
            "High Thumbnail Quality",
            "high_quality",
            "Use high quality thumbnails, will cause more data usage.",
            false
        ), SettingSwitch(
            "Use MP4 Format",
            "use_mp4_format",
            "Use MP4 formats for audio streams, turning it on may cause source errors.",
            false
        )
    )

    private lateinit var prefSettings: PrefSettings
    private lateinit var httpHelper: HttpHelper
    private lateinit var providers: Providers

    override fun init(prefSettings: PrefSettings, httpHelper: HttpHelper) {
        this.prefSettings = prefSettings
        this.httpHelper = httpHelper
        this.providers = Providers(
            M4UFree(httpHelper)
        )
    }

    override suspend fun onExtensionSelected() {

    }


  override suspend fun loadLinks(
    mediaItem: AVPMediaItem,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (StreamData) -> Unit
  ): Boolean {
    providers.getList().forEach { scraper ->
      withContext(Dispatchers.IO) {
        scraper.invoke(mediaItem, subtitleCallback, callback)
      }
    }
    return true
  }

  companion object {
    val metadata = ExtensionMetadata(
      className = "OfflineExtension",
      path = "",
      importType = ImportType.BuiltIn,
      name = "Ghost Extension",
      description = "the Sample of Built-in Extension",
      version = "1.0.0",
      author = "Avp",
      iconUrl = "https://www.freepnglogos.com/uploads/snapchat-logo-png-0.png",
    )
  }

}
