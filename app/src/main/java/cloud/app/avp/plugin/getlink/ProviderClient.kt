package cloud.app.avp.plugin.getlink

import cloud.app.avp.extension.provider.M4UFree
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.helpers.ImportType
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.ExtensionMetadata
import cloud.app.common.models.SubtitleData
import cloud.app.common.models.stream.StreamData
import cloud.app.common.settings.PrefSettings
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingSwitch

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
      scraper.invoke(mediaItem, subtitleCallback, callback)
    }
    return true
  }

  companion object {
    val metadata = ExtensionMetadata(
      className = "OfflineExtension",
      path = "",
      importType = ImportType.BuiltIn,
      id = "stream-built-in",
      name = "Ghost Extension",
      description = "the Sample of Built-in Extension",
      version = "1.0.0",
      author = "Avp",
      iconUrl = "https://www.freepnglogos.com/uploads/snapchat-logo-png-0.png",
    )
  }

}
