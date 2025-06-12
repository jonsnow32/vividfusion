package cloud.app.vvf.extension.builtIn.local

import android.content.Context
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.extension.plugger.LazyPluginRepo
import cloud.app.vvf.extension.plugger.catchLazy
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.extension.tmdb.TmdbTvdbClient
import kotlinx.coroutines.flow.MutableStateFlow

class BuiltInRepo(val context: Context): LazyPluginRepo<ExtensionMetadata, BaseClient> {
  override fun getAllPlugins() = MutableStateFlow(
    listOf(
      getLazy(BuiltInClient.metadata, BuiltInClient(context) ),
      getLazy(TmdbTvdbClient.metadata, TmdbTvdbClient() ),
    )
  )
  private  fun getLazy(extensionMetadata: ExtensionMetadata, client: BaseClient) =
    Result.success(Pair(extensionMetadata, catchLazy { client }))
}
