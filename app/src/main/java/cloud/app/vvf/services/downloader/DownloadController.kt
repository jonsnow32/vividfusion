package cloud.app.vvf.services.downloader

import cloud.app.vvf.common.models.DownloadItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Central controller for managing download states
 * This is the single source of truth for download state
 */
class DownloadController {

  // State machines for each download
  private val stateMachines = ConcurrentHashMap<String, DownloadStateMachine>()

  // Current download items exposed to UI
  private val _downloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
  val downloads: StateFlow<Map<String, DownloadItem>> = _downloads.asStateFlow()

  /**
   * Execute a download command
   * Returns true if command was accepted, false if invalid
   */
  fun executeCommand(command: DownloadCommand): Boolean {
    val downloadId = when (command) {
      is DownloadCommand.Start -> command.downloadId
      is DownloadCommand.Pause -> command.downloadId
      is DownloadCommand.Resume -> command.downloadId
      is DownloadCommand.Cancel -> command.downloadId
      is DownloadCommand.Remove -> command.downloadId
    }

    val stateMachine = stateMachines[downloadId] ?: run {
      if (command is DownloadCommand.Start) {
        // Create new state machine for new downloads
        DownloadStateMachine(downloadId).also {
          stateMachines[downloadId] = it
        }
      } else {
        Timber.w("No state machine found for download $downloadId")
        return false
      }
    }

    val newState = stateMachine.executeCommand(command)
    if (newState != null) {
      updateDownloadItem(downloadId, newState)
      return true
    }

    return false
  }

  /**
   * Handle events from WorkManager
   */
  fun handleWorkEvent(event: DownloadEvent): Boolean {
    val downloadId = when (event) {
      is DownloadEvent.WorkEnqueued -> event.downloadId
      is DownloadEvent.WorkStarted -> event.downloadId
      is DownloadEvent.ProgressUpdated -> event.downloadId
      is DownloadEvent.WorkCompleted -> event.downloadId
      is DownloadEvent.WorkFailed -> event.downloadId
      is DownloadEvent.WorkCancelled -> event.downloadId
    }

    val stateMachine = stateMachines[downloadId]
    if (stateMachine == null) {
      // Timber.w("No state machine found for work event: $event")
      // Silently ignore events for downloads without state machines (old downloads)
      return false
    }

    val newState = stateMachine.handleEvent(event)
    if (newState != null) {
      updateDownloadItem(downloadId, newState)
      return true
    }

    return false
  }

  /**
   * Update the DownloadItem based on current state
   */
  private fun updateDownloadItem(downloadId: String, state: DownloadState) {
    val currentDownloads = _downloads.value.toMutableMap()
    val existingItem = currentDownloads[downloadId]

    if (existingItem == null) {
      Timber.w("No existing DownloadItem for $downloadId")
      return
    }

    val stateMachine = stateMachines[downloadId] ?: return

    val extra = stateMachine.getExtraInfo() ?: emptyMap<String, Any>()

    val updatedItem = when (state) {
      is DownloadState.Running -> {
        val (progress, downloadedBytes, totalBytes) = stateMachine.getProgress() ?: Triple(
          0,
          0L,
          0L
        )

        when (existingItem) {
          is DownloadItem.HttpDownload -> existingItem.copyWith(
            status = state.toDownloadStatus(),
            progress = progress,
            downloadedBytes = downloadedBytes,
            fileSize = totalBytes,
            updatedAt = System.currentTimeMillis()
          )

          is DownloadItem.TorrentDownload -> existingItem.copyWith(
            status = state.toDownloadStatus(),
            progress = progress,
            downloadedBytes = downloadedBytes,
            fileSize = totalBytes,
            updatedAt = System.currentTimeMillis(),
          )

          is DownloadItem.HlsDownload -> existingItem.copyWith(
            status = state.toDownloadStatus(),
            progress = progress,
            downloadedBytes = downloadedBytes,
            fileSize = totalBytes,
            updatedAt = System.currentTimeMillis()
          )
        }
      }

      is DownloadState.Completed -> {
        val (localPath, fileSize) = stateMachine.getCompletionInfo() ?: Pair("", 0L)
        existingItem.copyWith(
          status = state.toDownloadStatus(),
          progress = 100,
          downloadedBytes = fileSize,
          fileSize = fileSize,
          localPath = localPath,
          updatedAt = System.currentTimeMillis()
        )
      }

      is DownloadState.Failed -> {
        val error = stateMachine.getError()
        existingItem.copyWith(
          status = state.toDownloadStatus(),
          updatedAt = System.currentTimeMillis()
        )
      }

      else -> {
        existingItem.copyWith(
          status = state.toDownloadStatus(),
          updatedAt = System.currentTimeMillis()
        )
      }
    }

    currentDownloads[downloadId] = updatedItem
    _downloads.value = currentDownloads

    Timber.d("Updated DownloadItem $downloadId: ${updatedItem.status} (${updatedItem.progress}%)")
  }

  /**
   * Add a new download item (called when starting new download)
   */
  fun addDownloadItem(downloadItem: DownloadItem) {
    val currentDownloads = _downloads.value.toMutableMap()
    currentDownloads[downloadItem.id] = downloadItem
    _downloads.value = currentDownloads

    // Create state machine if not exists
    if (!stateMachines.containsKey(downloadItem.id)) {
      stateMachines[downloadItem.id] = DownloadStateMachine(downloadItem.id)
    }
  }

  /**
   * Remove a download completely
   */
  fun removeDownload(downloadId: String) {
    val currentDownloads = _downloads.value.toMutableMap()
    currentDownloads.remove(downloadId)
    _downloads.value = currentDownloads

    stateMachines.remove(downloadId)
    Timber.d("Removed download $downloadId")
  }

  /**
   * Get current state of a download
   */
  fun getDownloadState(downloadId: String): DownloadState? {
    return stateMachines[downloadId]?.currentState
  }

  /**
   * Check if a download can be paused
   */
  fun canPause(downloadId: String): Boolean {
    return stateMachines[downloadId]?.currentState?.canPause() ?: false
  }

  /**
   * Check if a download can be resumed
   */
  fun canResume(downloadId: String): Boolean {
    return stateMachines[downloadId]?.currentState?.canResume() ?: false
  }

  /**
   * Get all active downloads
   */
  fun getActiveDownloads(): Set<String> {
    return stateMachines.entries
      .filter { it.value.currentState.isActive() }
      .map { it.key }
      .toSet()
  }

  /**
   * Initialize from persisted data
   */
  fun initializeFromPersistedData(downloadItems: List<DownloadItem>) {
    val downloadsMap = downloadItems.associateBy { it.id }
    _downloads.value = downloadsMap

    // Create state machines based on persisted status
    downloadItems.forEach { item ->
      val initialState = when (item.status) {
        cloud.app.vvf.common.models.DownloadStatus.PENDING -> DownloadState.Queued
        cloud.app.vvf.common.models.DownloadStatus.DOWNLOADING -> DownloadState.Running(
          item.progress, item.downloadedBytes, item.fileSize
        )

        cloud.app.vvf.common.models.DownloadStatus.PAUSED -> DownloadState.Paused
        cloud.app.vvf.common.models.DownloadStatus.COMPLETED -> DownloadState.Completed(
          item.localPath ?: "", item.fileSize
        )

        cloud.app.vvf.common.models.DownloadStatus.FAILED -> DownloadState.Failed("Previous error")
        cloud.app.vvf.common.models.DownloadStatus.CANCELLED -> DownloadState.Cancelled
      }

      stateMachines[item.id] = DownloadStateMachine(item.id, initialState)
    }

    Timber.d("Initialized DownloadController with ${downloadItems.size} downloads")
  }
}
