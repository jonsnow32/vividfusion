package cloud.app.vvf.common.clients.provider

import cloud.app.vvf.common.helpers.extractors.IExtractor
import cloud.app.vvf.common.models.extension.Message
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

interface ExtractorFlowProvider {
  fun setExtractorsFlow(extractorFlow: MutableStateFlow<List<IExtractor>>)
}
