package cloud.app.vvf.plugin

import cloud.app.vvf.extension.plugger.LazyPluginRepo
import cloud.app.vvf.extension.plugger.catchLazy
import cloud.app.vvf.plugin.getlink.ProviderClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.models.ExtensionMetadata
import kotlinx.coroutines.flow.MutableStateFlow

class StreamBuiltInExtensionRepo : LazyPluginRepo<ExtensionMetadata, StreamClient> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(ProviderClient.metadata, ProviderClient() ),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: StreamClient) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}
