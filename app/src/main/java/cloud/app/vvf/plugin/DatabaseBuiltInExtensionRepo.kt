package cloud.app.vvf.plugin

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.extension.plugger.LazyPluginRepo
import cloud.app.vvf.extension.plugger.catchLazy
import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.models.ExtensionMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DatabaseBuiltInExtensionRepo : LazyPluginRepo<ExtensionMetadata, DatabaseClient> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(BuiltInDatabaseClient.metadata, BuiltInDatabaseClient() ),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: DatabaseClient) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}

class BuiltInRepo: LazyPluginRepo<ExtensionMetadata, BaseClient> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(BuiltInDatabaseClient.metadata, BuiltInDatabaseClient() ),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: DatabaseClient) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}
