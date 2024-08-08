package cloud.app.avp.ui.main.movies

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import cloud.app.avp.base.CatchingViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.MediaItemsContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoviesViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {
  var moreFlow: PagedData<AVPMediaItem>? = null
  val flow = MutableStateFlow<PagingData<AVPMediaItem>?>(null)

  override fun onInitialize() {
    viewModelScope.launch {
      moreFlow?.toFlow()?.collectTo(flow)
    }
  }
}
