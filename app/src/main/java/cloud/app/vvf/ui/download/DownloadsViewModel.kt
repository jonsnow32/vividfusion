package cloud.app.vvf.ui.download

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.BuildConfig
import cloud.app.vvf.common.models.getMediaType
import cloud.app.vvf.datastore.app.AppDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import cloud.app.vvf.services.downloader.stateMachine.DownloadManager
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.DownloadStatus
import cloud.app.vvf.services.downloader.DownloadType

@HiltViewModel
class DownloadsViewModel @Inject constructor(
  private val downloadManager: DownloadManager,
  private val dataFlow: MutableStateFlow<AppDataStore>,
  private val application: Application
) : ViewModel() {

  val downloads: StateFlow<List<DownloadData>> = downloadManager.downloads
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
        val downloadsDir =
          android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)

        // Calculate storage info
        val storageInfo = StorageInfo.getDeviceStorageInfo(downloadsDir)
        _storageInfo.value = storageInfo

        Timber.d(
          "Storage info loaded - Total: ${StorageInfo.formatBytes(storageInfo.totalBytes)}, " +
            "Used: ${StorageInfo.formatBytes(storageInfo.usedBytes)}, " +
            "App: ${StorageInfo.formatBytes(storageInfo.appUsedBytes)}, " +
            "Free: ${StorageInfo.formatBytes(storageInfo.freeBytes)}"
        )
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
            BuildConfig.AUTHORITY_FILE_PROVIDER,
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

  fun playDownloadedFile(context: Context, downloadData: DownloadData) {
    if (downloadData.status != DownloadStatus.COMPLETED) {
      Timber.w("Cannot play file: download not completed")
      return
    }

    try {
      // Try multiple methods to locate the downloaded file
      val file = findDownloadedFile(context, downloadData)

      if (file == null || !file.exists()) {
        Timber.w("Downloaded file not found for: ${downloadData.fileName}")
        // Try to update the local path if file exists in downloads directory
        val foundFile = searchForFileInDownloadsDir(context, downloadData.fileName)
        if (foundFile?.exists() == true) {
          // Update the download item with correct path
          updateDownloadItemPath(downloadData, foundFile.absolutePath)
          playFileWithIntent(context, foundFile, downloadData)
        } else {
          Timber.e("File not found anywhere for: ${downloadData.fileName}")
        }
        return
      }

      playFileWithIntent(context, file, downloadData)

    } catch (e: Exception) {
      Timber.e(e, "Error playing downloaded file: ${downloadData.fileName}")
    }
  }

  /**
   * Try multiple methods to find the downloaded file
   */
  private fun findDownloadedFile(context: Context, downloadData: DownloadData): File? {
    // Method 1: Use localPath if available
    downloadData.localPath?.let { localPath ->
      if (localPath.isNotEmpty()) {
        val filePath = if (localPath.startsWith("file://")) {
          val uri = android.net.Uri.parse(localPath)
          uri.path ?: localPath.removePrefix("file://")
        } else {
          localPath
        }

        val file = File(filePath)
        if (file.exists()) {
          Timber.d("Found file using localPath: $filePath")
          return file
        }
      }
    }

    // Method 2: Search in downloads directory
    return searchForFileInDownloadsDir(context, downloadData.fileName)
  }

  /**
   * Search for file in the downloads directory
   */
  private fun searchForFileInDownloadsDir(context: Context, fileName: String?): File? {
    fileName ?: return null
    try {
      // Try app-specific downloads directory first
      val appDownloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
      appDownloadsDir?.let { dir ->
        // Try exact filename
        var file = File(dir, fileName)
        if (file.exists()) {
          Timber.d("Found file in app downloads: ${file.absolutePath}")
          return file
        }

        // Try with different extensions
        val baseFileName = fileName.substringBeforeLast(".")
        val extensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".webm", ".m4v", ".mp3", ".m4a")

        for (ext in extensions) {
          file = File(dir, "$baseFileName$ext")
          if (file.exists()) {
            Timber.d("Found file with extension $ext: ${file.absolutePath}")
            return file
          }
        }

        // Try VividFusion subfolder
        val vividDir = File(dir, "VividFusion")
        if (vividDir.exists()) {
          file = File(vividDir, fileName)
          if (file.exists()) {
            Timber.d("Found file in VividFusion subfolder: ${file.absolutePath}")
            return file
          }
        }
      }

      // Try public downloads directory (for older Android versions)
      if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(
          android.os.Environment.DIRECTORY_DOWNLOADS
        )

        val file = File(publicDownloads, fileName)
        if (file.exists()) {
          Timber.d("Found file in public downloads: ${file.absolutePath}")
          return file
        }
      }

    } catch (e: Exception) {
      Timber.e(e, "Error searching for file: $fileName")
    }

    return null
  }

  /**
   * Play the file using an intent
   */
  private fun playFileWithIntent(context: Context, file: File, downloadData: DownloadData) {
    try {
      // Use FileProvider to create a content URI that can be safely shared
      val contentUri = FileProvider.getUriForFile(
        context,
        BuildConfig.AUTHORITY_FILE_PROVIDER,
        file
      )

      // Get MIME type from file extension or media type
      val mimeType = when {
        file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm", "m4v") -> "video/*"
        file.extension.lowercase() in listOf("mp3", "m4a", "wav", "flac", "ogg") -> "audio/*"
        else -> when (downloadData.mediaItem?.getMediaType()) {
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
      Timber.d("Successfully launched player for: ${file.absolutePath}")

    } catch (e: Exception) {
      Timber.e(e, "Error launching player for file: ${file.absolutePath}")
    }
  }

  /**
   * Update download item with correct file path
   */
  private fun updateDownloadItemPath(downloadData: DownloadData, filePath: String) {
    try {
      // Update the download item based on its type
      val updatedItem = when (downloadData.type) {
        DownloadType.HTTP -> downloadData.copy(
          localPath = filePath,
          updatedAt = System.currentTimeMillis()
        )

        DownloadType.HLS -> downloadData.copy(
          localPath = filePath,
          updatedAt = System.currentTimeMillis()
        )

        DownloadType.TORRENT -> downloadData.copy(
          localPath = filePath,
          updatedAt = System.currentTimeMillis()
        )
      }

      // Save to datastore
      dataFlow.value.saveDownload(updatedItem)
      Timber.d("Updated download item path: $filePath")
    } catch (e: Exception) {
      Timber.e(e, "Error updating download item path")
    }
  }


  fun getDownloadProgress(downloadId: String) = downloadManager.getDownloadProgress(downloadId)

  fun getActiveDownloadsCount(): StateFlow<Int> =
    downloadManager.activeDownloads.let { activeFlow ->
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

  //
  fun openLocation(context: Context, downloadData: DownloadData) {
    val file = File(downloadData.localPath ?: return)
    if (!file.exists()) {
      Timber.w("File does not exist: ${file.absolutePath}")
      return
    }
    val parentDir = file.parentFile ?: return
    try {
      val uri = FileProvider.getUriForFile(
        context,
        BuildConfig.AUTHORITY_FILE_PROVIDER,
        parentDir
      )
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "resource/folder")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Timber.w(e, "No app can open folder directly, fallback to file manager")
      // Fallback: open a file manager with ACTION_GET_CONTENT
      try {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
          setDataAndType(android.net.Uri.parse(parentDir.absolutePath), "*/*")
          addCategory(Intent.CATEGORY_OPENABLE)
          if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        }
        context.startActivity(Intent.createChooser(intent, "Open folder with"))
      } catch (ex: Exception) {
        Timber.e(ex, "No file manager found to open folder")
      }
    }
  }
}
