package cloud.app.vvf.ui.download

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.common.models.DownloadItem
import cloud.app.vvf.common.models.DownloadStatus
import cloud.app.vvf.common.models.getMediaType
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.services.downloader.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
  private val downloadManager: DownloadManager,
  private val dataFlow: MutableStateFlow<AppDataStore>
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = combine(
        downloadManager.downloads,
        dataFlow.map { appDataStore -> appDataStore.getAllDownloads() ?: emptyList() }
    ) { managerDownloads, repoDownloads ->
        // Merge downloads from manager and repository, prioritizing manager data
        val mergedMap = mutableMapOf<String, DownloadItem>()

        // Add repository downloads first
        repoDownloads.forEach { item ->
            mergedMap[item.id] = item
        }

        // Override with manager downloads (more up-to-date)
        managerDownloads.values.forEach { item ->
            mergedMap[item.id] = item
        }

        mergedMap.values.sortedByDescending { it.updatedAt }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadManager.pauseDownload(downloadId)
                Timber.d("Paused download: $downloadId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to pause download: $downloadId")
            }
        }
    }

    fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadManager.resumeDownload(downloadId)
                Timber.d("Resumed download: $downloadId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to resume download: $downloadId")
            }
        }
    }

    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadManager.cancelDownload(downloadId)
                Timber.d("Cancelled download: $downloadId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to cancel download: $downloadId")
            }
        }
    }

    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadManager.retryDownload(downloadId)
                Timber.d("Retrying download: $downloadId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to retry download: $downloadId")
            }
        }
    }

    fun removeDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadManager.removeDownload(downloadId)
                dataFlow.value.removeDownload(downloadId)
                Timber.d("Removed download: $downloadId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove download: $downloadId")
            }
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            try {
                val completedDownloads = downloads.value.filter {
                    it.status == DownloadStatus.COMPLETED
                }

                completedDownloads.forEach { download ->
                    downloadManager.removeDownload(download.id)
                    dataFlow.value.removeDownload(download.id)
                }

                Timber.d("Cleared ${completedDownloads.size} completed downloads")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear completed downloads")
            }
        }
    }

    fun openDownloadsFolder(context: Context) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        downloadsDir
                    ),
                    "resource/folder"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Open Downloads Folder"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to open downloads folder")
            // Fallback: try to open default file manager
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                context.startActivity(Intent.createChooser(intent, "Browse Files"))
            } catch (fallbackException: Exception) {
                Timber.e(fallbackException, "Failed to open file manager")
            }
        }
    }

    fun playDownloadedFile(context: Context, downloadItem: DownloadItem) {
        if (downloadItem.status != DownloadStatus.COMPLETED || downloadItem.localPath == null) {
            Timber.w("Cannot play file: download not completed or path missing")
            return
        }

        try {
            val file = File(downloadItem.localPath!!) // Use non-null assertion since we checked above
            if (!file.exists()) {
                Timber.w("Downloaded file not found: ${downloadItem.localPath}")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = when (downloadItem.mediaItem.getMediaType()) {
                "movie", "episode", "video" -> "video/*"
                "audio" -> "audio/*"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Play with"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to play downloaded file: ${downloadItem.localPath}")
        }
    }

    fun getDownloadProgress(downloadId: String) = downloadManager.getDownloadProgress(downloadId)

    fun getActiveDownloadsCount(): StateFlow<Int> = downloadManager.activeDownloads.let { activeFlow ->
        kotlinx.coroutines.flow.flow {
            activeFlow.collect { activeSet ->
                emit(activeSet.size)
            }
        }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    }
}
