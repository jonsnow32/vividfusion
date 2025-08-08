package cloud.app.vvf.services.downloader.stateMachine

import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Central controller for managing download states
 * This is the single source of truth for download state
 * Now uses DownloadData as unified data structure
 */
class DownloadController {

  // State machines for each download
  private val stateMachines = ConcurrentHashMap<String, DownloadStateMachine>()

  // Current download data exposed to UI
  private val _downloads = MutableStateFlow<Map<String, DownloadData>>(emptyMap())
  val downloads: StateFlow<Map<String, DownloadData>> = _downloads.asStateFlow()

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
      updateDownloadData(downloadId, newState)
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
      // Silently ignore events for downloads without state machines (old downloads)
      return false
    }

    val newState = stateMachine.handleEvent(event)
    if (newState != null) {
      updateDownloadData(downloadId, newState)
      return true
    }

    return false
  }

  /**
   * Update the DownloadData based on current state
   */
  private fun updateDownloadData(downloadId: String, state: DownloadState) {
    val currentDownloads = _downloads.value.toMutableMap()
    val existingData = currentDownloads[downloadId]

    if (existingData == null) {
      Timber.w("No existing DownloadData for $downloadId")
      return
    }

    val updatedData = when (state) {
      is DownloadState.Running -> {
        val downloadData = state.downloadData

        // Merge the state downloadData with existing data, preserving ID and metadata
        downloadData.copy(
          id = existingData.id,
          mediaItem = existingData.mediaItem,
          url = existingData.url,
          status = state.toDownloadStatus(),
          createdAt = existingData.createdAt,
          updatedAt = System.currentTimeMillis(),
          type = existingData.type,

          // Merge type-specific data
          typeSpecificData = existingData.typeSpecificData + downloadData.typeSpecificData
        )
      }

      is DownloadState.Completed -> {
        val downloadData = state.downloadData
        existingData.copy(
          status = state.toDownloadStatus(),
          progress = 100,
          downloadedBytes = state.fileSize,
          totalBytes = state.fileSize,
          localPath = state.localPath,
          updatedAt = System.currentTimeMillis(),
          downloadSpeed = downloadData?.downloadSpeed ?: 0L
        )
      }

      is DownloadState.Failed -> {
        val downloadData = state.downloadData
        existingData.copy(
          status = state.toDownloadStatus(),
          updatedAt = System.currentTimeMillis(),
          progress = downloadData?.progress ?: existingData.progress,
          downloadedBytes = downloadData?.downloadedBytes ?: existingData.downloadedBytes
        )
      }

      else -> {
        existingData.copy(
          status = state.toDownloadStatus(),
          updatedAt = System.currentTimeMillis()
        )
      }
    }

    currentDownloads[downloadId] = updatedData
    _downloads.value = currentDownloads

    Timber.d("Updated DownloadData $downloadId: ${updatedData.status} (${updatedData.progress}%)")
  }

  /**
   * Add a new download data (called when starting new download)
   */
  fun addDownloadData(downloadData: DownloadData) {
    val currentDownloads = _downloads.value.toMutableMap()
    currentDownloads[downloadData.id] = downloadData
    _downloads.value = currentDownloads

    // Create state machine if not exists
    if (!stateMachines.containsKey(downloadData.id)) {
      stateMachines[downloadData.id] = DownloadStateMachine(downloadData.id)
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
  fun initializeFromPersistedData(downloadDataList: List<DownloadData>) {
    val downloadsMap = downloadDataList.associateBy { it.id }
    _downloads.value = downloadsMap

    // Create state machines based on persisted status
    downloadDataList.forEach { data ->
      val initialState = when (data.status) {
        DownloadStatus.PENDING -> DownloadState.Queued
        DownloadStatus.DOWNLOADING -> {
          DownloadState.Running(data)
        }
        DownloadStatus.PAUSED -> DownloadState.Paused
        DownloadStatus.COMPLETED -> DownloadState.Completed(
          data.localPath ?: "", data.totalBytes, data
        )
        DownloadStatus.FAILED -> DownloadState.Failed("Previous error", data)
        DownloadStatus.CANCELLED -> DownloadState.Cancelled
      }

      stateMachines[data.id] = DownloadStateMachine(data.id, initialState)
    }

    Timber.d("Initialized DownloadController with ${downloadDataList.size} downloads")
  }
}
