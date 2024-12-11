package cloud.app.vvf.extension.plugger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

class FileChangeListener(
    val scope: CoroutineScope,
) {
    val flow = MutableSharedFlow<File?>()
}
