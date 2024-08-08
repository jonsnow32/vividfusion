package cloud.app.avp.ui.main.search

import cloud.app.avp.base.CatchingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow){
}
