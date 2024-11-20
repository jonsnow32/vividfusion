package cloud.app.plugger

import kotlinx.coroutines.flow.StateFlow

interface PluginRepo<TMetadata, TPlugin> {
  fun load() : StateFlow<List<Result<Pair<TMetadata,Lazy<Result<TPlugin>>>>>>
}
