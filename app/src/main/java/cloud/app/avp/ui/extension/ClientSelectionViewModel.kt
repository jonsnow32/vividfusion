package cloud.app.avp.ui.extension

import cloud.app.avp.base.CatchingViewModel
import cloud.app.common.clients.Extension
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ClientSelectionViewModel(
    throwableFlow: MutableSharedFlow<Throwable>
) : CatchingViewModel(throwableFlow) {

    abstract val metadataFlow: StateFlow<List<Metadata>?>
    abstract val currentFlow : StateFlow<Extension<*>?>
    abstract fun onClientSelected(clientId: String)
}
