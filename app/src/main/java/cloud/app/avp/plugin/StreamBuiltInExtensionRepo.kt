package cloud.app.avp.plugin

import cloud.app.avp.extension.plugger.LazyPluginRepo
import cloud.app.avp.extension.plugger.catchLazy
import cloud.app.avp.plugin.getlink.ProviderClient
import cloud.app.common.clients.BaseClient
import cloud.app.common.clients.mvdatabase.DatabaseClient
import cloud.app.common.clients.streams.StreamClient
import cloud.app.common.models.ExtensionMetadata
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
