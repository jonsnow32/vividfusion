package cloud.app.avp.plugin

import cloud.app.avp.plugin.getlink.ProviderExtension
import cloud.app.common.clients.BaseExtension
import cloud.app.plugger.PluginRepo
import cloud.app.plugger.lazily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocalRepos : PluginRepo<BaseExtension> {
  override fun load(): StateFlow<List<Lazy<Result<BaseExtension>>>> = MutableStateFlow(
    listOf(
      lazily(TraktExtension()),
      lazily(TmdbExtension()),
      lazily(ProviderExtension())
    ))
}
