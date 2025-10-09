package cloud.app.vvf.extension.repo

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import kotlinx.coroutines.flow.Flow

interface ExtensionRepository {
    val flow: Flow<List<Result<Pair<ExtensionMetadata, Lazy<BaseClient>>>>?>
    suspend fun loadExtensions(): List<Result<Pair<ExtensionMetadata, Lazy<BaseClient>>>>
}
