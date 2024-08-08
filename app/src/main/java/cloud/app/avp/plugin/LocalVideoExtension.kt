package cloud.app.avp.plugin

import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.infos.FeedClient
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.Tab
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.Settings

class LocalVideoExtension : BaseExtension, FeedClient {
  override val settingItems: List<Setting> = listOf(
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
    TODO("Not yet implemented")
  }

  override suspend fun onExtensionSelected() {
    TODO("LOCAL EXTENSION Not yet implemented")
  }

  override suspend fun getHomeTabs(): List<Tab> = listOf("Alls, Download, Camera").map { Tab(it,it) }

  override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }
}
