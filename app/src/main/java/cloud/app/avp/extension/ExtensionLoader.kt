package cloud.app.avp.extension

import cloud.app.common.clients.BaseExtension
import kotlinx.coroutines.flow.MutableStateFlow

class ExtensionLoader(
  private val extensionFlow: MutableStateFlow<BaseExtension?>,
) {
}
