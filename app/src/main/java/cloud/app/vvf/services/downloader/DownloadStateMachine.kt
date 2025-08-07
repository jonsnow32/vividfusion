package cloud.app.vvf.services.downloader

import timber.log.Timber

/**
 * State Machine for managing download state transitions
 * This ensures valid state transitions and prevents race conditions
 */
class DownloadStateMachine(
    private val downloadId: String,
    initialState: DownloadState = DownloadState.Idle
) {
    private var _currentState: DownloadState = initialState
    val currentState: DownloadState get() = _currentState

    /**
     * Execute a command and return the new state
     * Returns null if the command is not valid for current state
     */
    fun executeCommand(command: DownloadCommand): DownloadState? {
        val newState = when (command) {
            is DownloadCommand.Start -> handleStart()
            is DownloadCommand.Pause -> handlePause()
            is DownloadCommand.Resume -> handleResume()
            is DownloadCommand.Cancel -> handleCancel()
            is DownloadCommand.Remove -> handleRemove()
        }

        if (newState != null) {
            val oldState = _currentState
            _currentState = newState
            Timber.d("Download $downloadId: State transition $oldState -> $newState")
        } else {
            Timber.w("Download $downloadId: Invalid command $command for state $_currentState")
        }

        return newState
    }

    /**
     * Handle events from WorkManager
     * This is separate from commands to avoid conflicts
     */
    fun handleEvent(event: DownloadEvent): DownloadState? {
        val newState = when (event) {
            is DownloadEvent.WorkEnqueued -> handleWorkEnqueued()
            is DownloadEvent.WorkStarted -> handleWorkStarted()
            is DownloadEvent.ProgressUpdated -> handleProgressUpdated(
                event.progress, event.downloadedBytes, event.totalBytes
            )
            is DownloadEvent.WorkCompleted -> handleWorkCompleted(
                event.localPath, event.fileSize
            )
            is DownloadEvent.WorkFailed -> handleWorkFailed(event.error)
            is DownloadEvent.WorkCancelled -> handleWorkCancelled()
        }

        if (newState != null) {
            val oldState = _currentState
            _currentState = newState
            Timber.d("Download $downloadId: Event transition $oldState -> $newState (${event::class.simpleName})")
        }

        return newState
    }

    // Command handlers
    private fun handleStart(): DownloadState? {
        return when (_currentState) {
            is DownloadState.Idle,
            is DownloadState.Failed,
            is DownloadState.Cancelled -> DownloadState.Queued
            else -> null
        }
    }

    private fun handlePause(): DownloadState? {
        return when (val state = _currentState) {
            is DownloadState.Queued -> DownloadState.Paused
            is DownloadState.Running -> DownloadState.Paused
            else -> null
        }
    }

    private fun handleResume(): DownloadState? {
        return when (_currentState) {
            is DownloadState.Paused -> DownloadState.Queued
            else -> null
        }
    }

    private fun handleCancel(): DownloadState? {
        return when (_currentState) {
            is DownloadState.Queued,
            is DownloadState.Running,
            is DownloadState.Paused -> DownloadState.Cancelled
            else -> null
        }
    }

    private fun handleRemove(): DownloadState? {
        // Remove is always allowed
        return DownloadState.Cancelled
    }

    // Event handlers
    private fun handleWorkEnqueued(): DownloadState? {
        return when (_currentState) {
            is DownloadState.Queued -> _currentState // No change
            else -> null
        }
    }

    private fun handleWorkStarted(): DownloadState? {
        return when (_currentState) {
            is DownloadState.Queued -> DownloadState.Running()
            else -> null
        }
    }

    private fun handleProgressUpdated(
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long,
    ): DownloadState? {
        return when (_currentState) {
            is DownloadState.Running -> DownloadState.Running(
                progress, downloadedBytes, totalBytes
            )
            is DownloadState.Queued -> DownloadState.Running(
                progress, downloadedBytes, totalBytes
            )
            else -> null
        }
    }

    private fun handleWorkCompleted(localPath: String, fileSize: Long): DownloadState? {
        return when (_currentState) {
            is DownloadState.Running -> DownloadState.Completed(localPath, fileSize)
            else -> null
        }
    }

    private fun handleWorkFailed(error: String): DownloadState? {
        return when (_currentState) {
            is DownloadState.Queued,
            is DownloadState.Running -> DownloadState.Failed(error)
            // Don't override PAUSED state with FAILED when work fails due to pause
            is DownloadState.Paused -> null
            else -> null
        }
    }

    private fun handleWorkCancelled(): DownloadState? {
        return when (_currentState) {
            is DownloadState.Queued,
            is DownloadState.Running -> DownloadState.Cancelled
            // Don't override PAUSED state with CANCELLED when work is cancelled due to pause
            is DownloadState.Paused -> null
            else -> null
        }
    }

    /**
     * Get progress information if available
     */
    fun getProgress(): Triple<Int, Long, Long>? {
        return when (val state = _currentState) {
            is DownloadState.Running -> Triple(state.progress, state.downloadedBytes, state.totalBytes)
            else -> null
        }
    }
    /**
     * Get completion information if available
     */
    fun getCompletionInfo(): Pair<String, Long>? {
        return when (val state = _currentState) {
            is DownloadState.Completed -> Pair(state.localPath, state.fileSize)
            else -> null
        }
    }

    /**
     * Get error information if available
     */
    fun getError(): String? {
        return when (val state = _currentState) {
            is DownloadState.Failed -> state.error
            else -> null
        }
    }
}
