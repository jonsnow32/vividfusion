package cloud.app.avp.ui.main.home

import cloud.app.avp.base.CatchingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ShowsViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

  val genres = MutableStateFlow<List<String>>(emptyList())
  var selectedTab = "Action"
  init {
    genres.value = listOf(
      "Progress",
      "Upcoming",
      "WatchList",
      "History",
    )
  }
}
