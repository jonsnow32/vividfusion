package cloud.app.avp.plugin.getlink

import cloud.app.avp.extension.provider.M4UFree
import cloud.app.common.clients.BaseClient
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.stream.StreamData
import cloud.app.common.settings.PrefSettings
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ProviderClient:  BaseClient , StreamClient {


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

    override suspend fun searchStreams(mediaItem: AVPMediaItem): Flow<List<StreamData>> {
        return channelFlow {
            val producer = this;
            providers.getList().forEach { scraper ->
                launch(Dispatchers.IO) {
                    Timber.i("begin loading ${scraper.name}")
                    scraper.invoke(mediaItem, producer)
                    Timber.i("end loading ${scraper.name}")
                }
            }
        }
    }


}
