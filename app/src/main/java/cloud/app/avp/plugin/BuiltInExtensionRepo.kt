package cloud.app.avp.plugin

import cloud.app.avp.extension.plugger.LazyPluginRepo
import cloud.app.avp.extension.plugger.catchLazy
import cloud.app.common.clients.BaseClient
import cloud.app.common.models.ExtensionMetadata
import kotlinx.coroutines.flow.MutableStateFlow

class BuiltInExtensionRepo<T: BaseClient> : LazyPluginRepo<ExtensionMetadata, T> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(BuiltInDatabaseClient.metadata, BuiltInDatabaseClient() as T),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: T) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}
