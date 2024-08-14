package cloud.app.avp.extension

import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.Settings

class SampleClient : BaseExtension, StreamClient {

  override val metadata: ExtensionMetadata
    get() = ExtensionMetadata(
      name = "SampleClient",
      ExtensionType.DATABASE,
      description = "A sample extension that does nothing",
      author = "avp",
      version = "v001",
      icon = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png",
      loginType = LoginType.NONE

    )
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
  override fun setSettings(settings: Settings) {

  }

  override suspend fun onExtensionSelected() {

  }

  override suspend fun searchStreams() {

  }
}
