package cloud.app.vvf.ui.widget.dialog

import cloud.app.vvf.base.CatchingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class SelectionViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
) : CatchingViewModel(throwableFlow)
