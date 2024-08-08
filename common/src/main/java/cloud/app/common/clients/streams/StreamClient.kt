package cloud.app.common.clients.streams

interface StreamClient {
  suspend fun searchStreams()
}
