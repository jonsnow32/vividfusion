package cloud.app.plugger

import cloud.app.plugger.utils.combineStates
import kotlinx.coroutines.flow.StateFlow

class RepoComposer<TPlugin>(private vararg val repos: PluginRepo<TPlugin>) : PluginRepo<TPlugin> {
  override fun load() = repos.map {
    it.load()
  }.reduce { a, b ->
    combineStates(a, b) { x, y ->
      x + y
    }
  }
}
