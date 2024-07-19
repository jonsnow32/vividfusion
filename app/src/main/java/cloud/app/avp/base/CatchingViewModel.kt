package cloud.app.avp.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cloud.app.avp.utils.catchWith
import cloud.app.avp.utils.tryWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class CatchingViewModel(
    val throwableFlow: MutableSharedFlow<Throwable>
) : ViewModel() {
    private var initialized = false
    open fun onInitialize() {}

    fun initialize() {
        if (initialized) return
        initialized = true
        onInitialize()
    }

    suspend fun <T> tryWith(block: suspend () -> T) = tryWith(throwableFlow, block)
    suspend fun <T : Any> Flow<PagingData<T>>.collectTo(
        collector: FlowCollector<PagingData<T>>
    ) = cachedIn(viewModelScope).catchWith(throwableFlow).collect(collector)

}
