package cloud.app.vvf.extension.builtIn

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.extension.plugger.LazyPluginRepo
import cloud.app.vvf.extension.plugger.catchLazy
import cloud.app.vvf.common.models.ExtensionMetadata
import kotlinx.coroutines.flow.MutableStateFlow

class BuiltInRepo: LazyPluginRepo<ExtensionMetadata, BaseClient> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(BuiltInClient.metadata, BuiltInClient() ),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: BaseClient) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}
