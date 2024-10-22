package cloud.app.plugger

import kotlinx.coroutines.flow.StateFlow

interface PluginRepo<TPlugin> {
  fun load() : StateFlow<List<Lazy<Result<TPlugin>>>>
}
fun <T> lazily(value: T) = lazy { runCatching { value } }
