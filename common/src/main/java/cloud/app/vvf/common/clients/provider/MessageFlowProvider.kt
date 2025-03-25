package cloud.app.vvf.common.clients.provider

import cloud.app.vvf.common.models.Message
import kotlinx.coroutines.flow.MutableSharedFlow

interface MessageFlowProvider {
  fun setMessageFlow(messageFlow: MutableSharedFlow<Message>)
}
