package cloud.app.avp.extension

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

class SampleClient : StreamClient {
  private lateinit var httpHelper: HttpHelper

  override fun init(prefSettings: PrefSettings, httpHelper: HttpHelper) {
    this.httpHelper = httpHelper;
  }

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

  override suspend fun onExtensionSelected() {

  }

  private fun fakeList(i: Int): List<StreamData> {
    return listOf(
      StreamData(
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample${i}/BigBuckBunny.mp4",
        resolvedUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        fileName = "BigBuckBunny.mp4"
      )
    )
  }

  override suspend fun loadLinks(
    mediaItem: AVPMediaItem,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (StreamData) -> Unit
  ): Boolean {

//    providers.getList().forEach { scraper ->
//      scraper.invoke(mediaItem, subtitleCallback, callback)
//    }
    return true
  }

  companion object {
    val metadata: ExtensionMetadata
      get() = ExtensionMetadata(
        className = "cloud.app.avp.extension.SampleClient",
        path = "",
        importType = ImportType.App,
        id = "Test stream client",
        description = "A sample of stream client",
        name = "Extension Sample",
        author = "avp",
        version = "v001",
        iconUrl = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png",
      )
  }
}
