package cloud.app.vvf.extension.repo

import android.content.Context
import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.WeakHashMap

class FileRepository(
    private val folder: File,
    private val parser: ExtensionParser,
    private val fileIgnoreFlow: Flow<File?>
) : ExtensionRepository {

    private val map =
        WeakHashMap<String, Pair<String, Result<Pair<ExtensionMetadata, Lazy<BaseClient>>>>>()
    private val mutex = Mutex()

    private var toIgnoreFile: File? = null
    override val flow = channelFlow {
        send(loadExtensions())
        fileIgnoreFlow.collectLatest {
            toIgnoreFile = it
            send(loadExtensions())
        }
    }.flowOn(Dispatchers.IO)

    private fun loadAllApks() = folder.run {
        setReadOnly()
        listFiles()!!.filter {
            it != toIgnoreFile && it.extension == "apk"
        }.onEach { it.setWritable(false) }
    }

    override suspend fun loadExtensions() = mutex.withLock {
        parser.getAllDynamically(ImportType.File, map, loadAllApks())
    }

    constructor(
        context: Context, parser: ExtensionParser, fileIgnoreFlow: Flow<File?>
    ) : this(context.getExtensionsFileDir(), parser, fileIgnoreFlow)

    companion object {
        fun Context.getExtensionsFileDir() = File(filesDir, "extensions").apply { mkdirs() }
    }
}
