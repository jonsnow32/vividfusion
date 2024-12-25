package cloud.app.vvf.extension.plugger

import tel.jeelpa.plugger.PluginRepo
import tel.jeelpa.plugger.utils.combineStates
import tel.jeelpa.plugger.utils.mapState
import cloud.app.vvf.common.models.ExtensionMetadata

interface LazyPluginRepo<T, R> : PluginRepo<T, Lazy<Result<R>>>

class LazyRepoComposer<TPlugin>(
    private vararg val repos: LazyPluginRepo<ExtensionMetadata, TPlugin>
) : LazyPluginRepo<ExtensionMetadata, TPlugin> {
    override fun getAllPlugins() = repos.map { it.getAllPlugins() }
        .reduce { a, b -> combineStates(a, b) { x, y -> x + y } }
        .mapState { list ->
            list.groupBy { it.getOrNull()?.first?.className }.map { entry ->
                entry.value.minBy {
                    it.getOrNull()?.first?.importType?.ordinal ?: Int.MAX_VALUE
                }
            }
        }
}

fun <T> catchLazy(function: () -> T) = lazy { runCatching { function() } }
