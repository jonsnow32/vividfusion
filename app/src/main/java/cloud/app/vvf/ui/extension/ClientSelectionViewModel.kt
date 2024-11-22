package cloud.app.vvf.ui.extension

import cloud.app.vvf.base.CatchingViewModel
import cloud.app.vvf.common.clients.Extension
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ClientSelectionViewModel(
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    abstract val metadataFlow: StateFlow<List<Metadata>?>
    abstract val currentFlow : StateFlow<Extension<*>?>
    abstract fun onClientSelected(clientId: String)
}
