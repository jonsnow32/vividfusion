package cloud.app.avp.plugin.getlink

import cloud.app.avp.extension.provider.M4UFree
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.models.stream.StreamData
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ProviderExtension:  BaseExtension , StreamClient {
    override val metadata: ExtensionMetadata
        get() = ExtensionMetadata(
            name = "The extension of Providers",
            ExtensionType.STREAM,
            description = "A sample extension that does nothing",
            author = "avp",
            version = "v001",
            icon = "https://www.freepnglogos.com/uploads/netflix-logo-0.png",
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
    private lateinit var settings: Settings
    private lateinit var httpHelper: HttpHelper
    private lateinit var providers: Providers
    override fun init(settings: Settings, httpHelper: HttpHelper) {
        this.settings = settings
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