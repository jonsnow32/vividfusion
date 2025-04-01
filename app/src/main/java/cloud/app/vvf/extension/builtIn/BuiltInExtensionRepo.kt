package cloud.app.vvf.extension.builtIn

import android.content.Context
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.extension.plugger.LazyPluginRepo
import cloud.app.vvf.extension.plugger.catchLazy
import cloud.app.vvf.common.models.ExtensionMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Cache

class BuiltInRepo(val context: Context): LazyPluginRepo<ExtensionMetadata, BaseClient> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(BuiltInClient.metadata, BuiltInClient(context) ),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: BaseClient) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}
