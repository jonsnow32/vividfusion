package cloud.app.common.clients.streams

import cloud.app.common.clients.BaseClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.stream.StreamData
import kotlinx.coroutines.flow.Flow

interface StreamClient : BaseClient {
  suspend fun searchStreams(mediaItem: AVPMediaItem) : Flow<List<StreamData>>
}
