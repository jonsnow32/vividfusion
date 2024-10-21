package cloud.app.avp.plugin

import cloud.app.avp.plugin.tvdb.AppTheTvdb
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.clients.mvdatabase.ShowClient
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Ids
import cloud.app.common.models.movie.Season
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingList
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.PrefSettings

class TvdbExtension : BaseExtension, ShowClient {
  private lateinit var tvdb: AppTheTvdb
  override val metadata: ExtensionMetadata
    get() = ExtensionMetadata(
      name = "The extension of TvDB",
      ExtensionType.DATABASE,
      description = "extension to of tvdb",
      author = "avp",
      version = "v001",
      icon = "https://www.freepnglogos.com/uploads/netflix-logo-0.png",
      loginType = LoginType.API_KEY
    )

  private lateinit var prefSettings: PrefSettings
  override fun init(prefSettings: PrefSettings, httpHelper: HttpHelper) {
    this.prefSettings = prefSettings
    tvdb = AppTheTvdb(
      httpHelper.okHttpClient,
      prefSettings.getString("pref_tvdb_api_key") ?: "4ef60b9d635f533695cbcaccb6603a57",
      prefSettings,
    )
  }

  override suspend fun onExtensionSelected() {

  }

  override val defaultSettings: List<Setting> = listOf(
    SettingSwitch(
      "Include Adult",
      "tvdb_include_adult",
      "Include adult",
      false
    ),
    SettingList(
      "Language",
      "tvdb_language",
      "Language",
      entryTitles = listOf("English", "Spanish"),
      entryValues = listOf("en", "es")
    ),
    SettingList(
      "Region",
      "tvdb_Region",
      "Region",
      entryTitles = listOf("English", "Spanish"),
      entryValues = listOf("en", "es")
    )
  )

  override suspend fun getSeasons(showIds: Ids): List<Season> {
    TODO("Not yet implemented")
  }

  override suspend fun getEpisodes(showIds: Ids, seasonNumber: Int?): List<Episode> {
    TODO("Not yet implemented")
  }
}
