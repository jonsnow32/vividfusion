package cloud.app.vvf.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cloud.app.vvf.utils.catchWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

abstract class CatchingViewModel(
  val throwableFlow: MutableSharedFlow<Throwable>
) : ViewModel() {

  private var initialized = false

  open fun onInitialize() {}

  private fun initialize() {
    if (initialized) return
    initialized = true
    onInitialize()
  }

  @Inject
  fun postInject() {
    initialize() // Ensure initialization happens after dependencies are injected
  }
  suspend fun <T : Any> Flow<PagingData<T>>.collectTo(
    collector: FlowCollector<PagingData<T>>
  ) = cachedIn(viewModelScope).catchWith(throwableFlow).collect(collector)
}
