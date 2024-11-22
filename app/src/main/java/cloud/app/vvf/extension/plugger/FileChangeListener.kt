package cloud.app.vvf.extension.plugger

import cloud.app.vvf.common.models.ExtensionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

class FileChangeListener(
    val scope: CoroutineScope,
) {
    val map = mutableMapOf<ExtensionType, MutableSharedFlow<File?>>()
    fun getFlow(type: ExtensionType) = map.getOrPut(type) { MutableSharedFlow() }
}