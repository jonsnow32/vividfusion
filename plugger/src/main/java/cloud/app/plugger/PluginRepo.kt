package cloud.app.plugger

import kotlinx.coroutines.flow.StateFlow

interface PluginRepo<TPlugin> {
  fun load() : StateFlow<List<TPlugin>>
}
