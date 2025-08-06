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
import cloud.app.vvf.utils.KUniFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class DownloadsViewModel @Inject constructor(
  private val downloadManager: DownloadManager,
  private val dataFlow: MutableStateFlow<AppDataStore>
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloads
        .map { downloadsMap ->
            val sortedList = downloadsMap.values.sortedByDescending { it.updatedAt }
            Timber.d("DownloadsViewModel: Downloads updated - Count: ${sortedList.size}")
            sortedList.forEach { item ->
                Timber.d("DownloadsViewModel: Download ${item.id} - Status: ${item.status} - Progress: ${item.progress}%")
            }
            sortedList
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Storage information state
    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo

    init {
        // Load storage information when ViewModel is created
        loadStorageInfo()
    }

    /**
     * Load device storage information and update UI
     */
    fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                // Get downloads directory from Environment or app-specific directory
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)

                // Calculate storage info
                val storageInfo = StorageInfo.getDeviceStorageInfo(downloadsDir)
                _storageInfo.value = storageInfo

                Timber.d("Storage info loaded - Total: ${StorageInfo.formatBytes(storageInfo.totalBytes)}, " +
                        "Used: ${StorageInfo.formatBytes(storageInfo.usedBytes)}, " +
                        "App: ${StorageInfo.formatBytes(storageInfo.appUsedBytes)}, " +
                        "Free: ${StorageInfo.formatBytes(storageInfo.freeBytes)}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load storage information")
                _storageInfo.value = null
            }
        }
    }

    /**
     * Refresh storage information (call this when downloads complete/are deleted)
     */
    fun refreshStorageInfo() {
        loadStorageInfo()
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                Timber.d("DownloadsViewModel: About to pause download: $downloadId")
                val beforePause = downloadManager.downloads.value[downloadId]
                Timber.d("DownloadsViewModel: Status before pause: ${beforePause?.status}")

                downloadManager.pauseDownload(downloadId)

                // Wait a bit and check status after pause
                kotlinx.coroutines.delay(100)
                val afterPause = downloadManager.downloads.value[downloadId]
                Timber.d("DownloadsViewModel: Status after pause: ${afterPause?.status}")

                Timber.d("DownloadsViewModel: Paused download: $downloadId")
            } catch (e: Exception) {
                Timber.e(e, "DownloadsViewModel: Failed to pause download: $downloadId")
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
                        "${context.packageName}.fileprovider",
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
            // Parse the local path to get the actual file path
            val filePath = if (downloadItem.localPath!!.startsWith("file://")) {
                val uri = android.net.Uri.parse(downloadItem.localPath!!)
                uri.path ?: downloadItem.localPath!!.removePrefix("file://")
            } else {
                downloadItem.localPath!!
            }

            val file = File(filePath)

            if (!file.exists()) {
                Timber.w("Downloaded file not found: $filePath")
                return
            }

            // Use FileProvider to create a content URI that can be safely shared
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Get MIME type from file extension or media type
            val mimeType = when {
                file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm", "m4v") -> "video/*"
                file.extension.lowercase() in listOf("mp3", "m4a", "wav", "flac", "ogg") -> "audio/*"
                else -> when (downloadItem.mediaItem.getMediaType()) {
                    "movie", "episode", "video" -> "video/*"
                    "audio" -> "audio/*"
                    else -> "*/*"
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Play with"))
            Timber.d("Successfully launched player for: $filePath")
            Timber.d("File size: ${file.length()} bytes, MIME type: $mimeType")
            Timber.d("Content URI: $contentUri")

        } catch (e: Exception) {
            Timber.e(e, "Failed to play downloaded file: ${downloadItem.localPath}")

            // Additional fallback: try using KUniFile but convert to FileProvider URI
            try {
                Timber.d("Attempting KUniFile fallback method...")

                val uniFile = try {
                    val uri = android.net.Uri.parse(downloadItem.localPath!!)
                    KUniFile.fromUri(context, uri)
                } catch (ex: Exception) {
                    val file = File(downloadItem.localPath!!)
                    KUniFile.fromFile(context, file)
                }

                if (uniFile != null && uniFile.exists()) {
                    // Get the actual file if possible
                    val actualFile = try {
                        File(uniFile.filePath ?: throw IllegalStateException("No file path"))
                    } catch (ex: Exception) {
                        null
                    }

                    if (actualFile != null && actualFile.exists()) {
                        val fallbackUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            actualFile
                        )

                        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fallbackUri, uniFile.type ?: "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(Intent.createChooser(fallbackIntent, "Play with"))
                        Timber.d("KUniFile fallback successful for: ${actualFile.absolutePath}")
                    } else {
                        Timber.w("KUniFile fallback: actual file not accessible")
                    }
                } else {
                    Timber.w("KUniFile fallback: file not found or doesn't exist")
                }
            } catch (fallbackException: Exception) {
                Timber.e(fallbackException, "All fallback methods failed")
            }
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
