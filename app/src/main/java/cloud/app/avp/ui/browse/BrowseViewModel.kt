package cloud.app.avp.ui.browse

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {
  var moreFlow: PagedData<AVPMediaItem>? = null
  var title: String? = null
  val flow = MutableStateFlow<PagingData<AVPMediaItem>?>(null)
  val loading = MutableSharedFlow<Boolean>()
  override fun onInitialize() {
    viewModelScope.launch {
      moreFlow?.toFlow()?.collectTo(flow)
    }
  }
  fun refresh() {
    viewModelScope.launch {
      loading.emit(true)
      moreFlow?.clear()
      loading.emit(false)
    }
  }
}
