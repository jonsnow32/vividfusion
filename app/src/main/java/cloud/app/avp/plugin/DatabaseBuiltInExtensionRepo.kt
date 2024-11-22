package cloud.app.avp.plugin

import cloud.app.avp.extension.plugger.LazyPluginRepo
import cloud.app.avp.extension.plugger.catchLazy
import cloud.app.common.clients.mvdatabase.DatabaseClient
import cloud.app.common.models.ExtensionMetadata
import kotlinx.coroutines.flow.MutableStateFlow

class DatabaseBuiltInExtensionRepo : LazyPluginRepo<ExtensionMetadata, DatabaseClient> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(BuiltInDatabaseClient.metadata, BuiltInDatabaseClient() ),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: DatabaseClient) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}
