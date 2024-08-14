package cloud.app.avp.plugin

import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.Tab
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.Settings
import kotlin.math.log

class TraktExtension : BaseExtension, FeedClient {
  override val metadata: ExtensionMetadata
    get() = ExtensionMetadata(
      name = "Trakt Extension",
      ExtensionType.DATABASE,
      description = "This extension integrates the popular Trakt.tv service into your application, providing users with a seamless way to track, discover, and manage their favorite movies, TV shows, and anime.",
      author = "avp",
      version = "v001",
      icon = "https://www.freepnglogos.com/uploads/netflix-logo-0.png",
      loginType = LoginType.WEBAUTH
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

  override suspend fun getHomeTabs(): List<Tab> = listOf("Alls, Download, Camera").map { Tab(it,it) }

  override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }
}
