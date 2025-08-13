package cloud.app.vvf.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import cloud.app.vvf.ExtensionOpenerActivity.Companion.openExtensionInstaller
import timber.log.Timber
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.MainActivity
import cloud.app.vvf.ui.detail.torrent.TorrentInfoFragment
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.common.models.AVPMediaItem

/**
 * Handles various types of intents and URIs for the application
 */
class IntentHandler(private val mainActivity: MainActivity) {

  /**
   * Main entry point for handling intents
   */
  fun handleIntent(intent: Intent?) {
    intent ?: return

    val uri = intent.data
    val action = intent.action
    val type = intent.type

    Timber.d("Handling intent - Action: $action, URI: $uri, Type: $type")

    when {
      // Handle URI-based intents
      uri != null -> handleUriIntent(uri)

      // Handle file-based intents without URI
      action == Intent.ACTION_VIEW && type != null -> handleMimeTypeIntent(intent, type)

      // Handle other actions
      else -> handleOtherIntent(intent)
    }
  }

  private fun handleUriIntent(uri: Uri) {
    when (uri.scheme?.lowercase()) {
      // Internal app scheme
      "vvf" -> {
        Timber.d("Handling VVF scheme: $uri")
        handleInternalUri(uri)
      }

      // Web links
      "http", "https" -> {
        Timber.d("Handling web link: $uri")
        handleWebLink(uri)
      }

      // Torrent magnet links
      "magnet" -> {
        Timber.d("Handling magnet link: $uri")
        handleTorrentUri(uri)
      }

      // File system
      "file" -> {
        Timber.d("Handling file: $uri")
        handleFileUri(uri)
      }

      // Content provider
      "content" -> {
        Timber.d("Handling content: $uri")
        handleContentUri(uri)
      }

      else -> {
        Timber.w("Unsupported URI scheme: ${uri.scheme} for URI: $uri")
        handleUnsupportedScheme(uri)
      }
    }
  }

  private fun handleInternalUri(uri: Uri) {
    // Handle internal app URIs (vvf://)

  }


  private fun handleWebLink(uri: Uri) {
    when (uri.host?.lowercase()) {
      "avp.repo" -> {
        Timber.d("Opening AVP repository link: $uri")
        // Special domain for the app
      }

      else -> {
        // External web links
        Timber.d("Opening external web link: $uri")
        handleExternalWebLink(uri)
      }
    }
  }

  private fun handleTorrentUri(uri: Uri) {
    Timber.i("Torrent URI received: $uri")
    // TODO: Implement torrent handling logic
    // Examples:
    // - Start torrent download
    // - Show torrent details dialog
    // - Add to download queue
    // - Open with external torrent client
    val fragment = TorrentInfoFragment()
    val bundle = Bundle()
    bundle.putParcelable("uri", uri)
    fragment.arguments = bundle
    mainActivity.navigate(fragment)
  }

  private fun handleExternalWebLink(uri: Uri) {
    Timber.i("External web link received: $uri")
    // TODO: Implement external web link handling
    // Examples:
    // - Open in browser
    // - Show link preview
    // - Check if it's a supported streaming link
    // - Show chooser dialog
  }


  private fun handleExtension(uri: Uri) {
    Timber.i("Extension received: $uri")
    // TODO: Implement Extension handling
    // Examples:
    // - Open in browser
    // - Show link preview
    // - Check if it's a supported streaming link
    // - Show chooser dialog
  }

  private fun handleFileUri(uri: Uri) {
    val path = uri.path
    when {
      path?.endsWith(".torrent", ignoreCase = true) == true -> {
        Timber.d("Handling torrent file: $path")
        handleTorrentUri(uri)
      }

      path?.endsWith(".apk", ignoreCase = true) == true -> {
        Timber.d("Handling APK file (extension): $path")
        handleExtension(uri)
      }

      // Handle video files
      isVideoFile(path) -> {
        Timber.d("Handling video file: $path")
        handleMediaFile(uri, "video")
      }

      // Handle audio files
      isAudioFile(path) -> {
        Timber.d("Handling audio file: $path")
        handleMediaFile(uri, "audio")
      }

      else -> {
        Timber.d("Handling generic file: $path")
        handleGenericFile(uri)
      }
    }
  }

  private fun handleContentUri(uri: Uri) {
    val path = uri.path
    val lastPathSegment = uri.lastPathSegment

    Timber.d("Content URI path: $path, lastPathSegment: $lastPathSegment")

    when {
      path?.endsWith(".torrent", ignoreCase = true) == true ||
        lastPathSegment?.endsWith(".torrent", ignoreCase = true) == true -> {
        Timber.d("Handling torrent file from content provider: $uri")
        handleTorrentUri(uri)
      }

      path?.endsWith(".apk", ignoreCase = true) == true ||
        lastPathSegment?.endsWith(".apk", ignoreCase = true) == true -> {
        Timber.d("Handling APK file from content provider: $uri")
        handleExtension(uri)
      }

      // Handle video files from content provider
      isVideoFile(path) || isVideoFile(lastPathSegment) -> {
        Timber.d("Handling video file from content provider: $uri")
        handleMediaFile(uri, "video")
      }

      // Handle audio files from content provider
      isAudioFile(path) || isAudioFile(lastPathSegment) -> {
        Timber.d("Handling audio file from content provider: $uri")
        handleMediaFile(uri, "audio")
      }

      else -> {
        Timber.d("Handling generic content: $uri")
        handleGenericFile(uri)
      }
    }
  }

  private fun handleGenericFile(uri: Uri) {
    Timber.i("Generic file received: $uri")

    // Try to determine if it's a media file by checking content resolver
    try {
      val contentResolver = mainActivity.contentResolver
      var mimeType = contentResolver.getType(uri)

      // Try to guess MIME type from file extension if not found or is generic
      if (mimeType == null || mimeType == "application/octet-stream") {
        val fileName = getFileNameFromUri(uri)
        val extension = fileName?.substringAfterLast('.', "")?.lowercase()
        if (!extension.isNullOrEmpty()) {
          val guessedMime = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
          if (!guessedMime.isNullOrEmpty()) {
            mimeType = guessedMime
          }
        }
      }

      when {
        mimeType?.startsWith("video/") == true -> {
          Timber.d("Detected video file via MIME type: $mimeType")
          handleMediaFile(uri, "video")
        }
        mimeType?.startsWith("audio/") == true -> {
          Timber.d("Detected audio file via MIME type: $mimeType")
          handleMediaFile(uri, "audio")
        }
        // Handle torrent files by extension
        else -> {
          val fileName = getFileNameFromUri(uri)
          val extension = fileName?.substringAfterLast('.', "")?.lowercase()
          if (extension == "torrent") {
            Timber.d("Detected torrent file via extension")
            handleTorrentUri(uri)
          } else {
            Timber.d("Unknown file type - MIME: $mimeType, URI: $uri")
            // Could show a dialog asking user what to do with the file
          }
        }
      }
    } catch (e: Exception) {
      Timber.e(e, "Failed to determine file type for: $uri")
    }
  }

  private fun handleMimeTypeIntent(intent: Intent, mimeType: String) {
    Timber.d("Handling MIME type: $mimeType")
    when {
      mimeType.startsWith("video/") -> {
        // Handle video files
        intent.data?.let {
          Timber.d("Handling video file with MIME type: $mimeType")
          handleMediaFile(it, "video")
        }
      }

      mimeType.startsWith("audio/") -> {
        // Handle audio files
        intent.data?.let {
          Timber.d("Handling audio file with MIME type: $mimeType")
          handleMediaFile(it, "audio")
        }
      }

      mimeType == "application/x-bittorrent" -> {
        // Handle torrent files
        intent.data?.let { handleTorrentUri(it) }
      }

      mimeType == "application/vnd.android.package-archive" -> {
        // Handle APK files (extensions)
        intent.data?.let { handleExtension(it) }
      }

      else -> {
        Timber.w("Unsupported MIME type: $mimeType")
        intent.data?.let { handleUnsupportedScheme(it) }
      }
    }
  }

  private fun handleOtherIntent(intent: Intent) {
    when (intent.action) {
      Intent.ACTION_MAIN -> {
        // App launched normally
        Timber.d("App launched normally")
      }

      Intent.ACTION_SEND -> {
        // Handle shared content
        Timber.d("Handling shared content")
        handleSharedContent(intent)
      }

      else -> {
        Timber.w("Unhandled intent action: ${intent.action}")
      }
    }
  }

  private fun handleSharedContent(intent: Intent) {
    when {
      intent.type?.startsWith("text/") == true -> {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        sharedText?.let { text ->
          // Try to parse as URI
          try {
            val uri = text.toUri()
            if (uri.scheme != null) {
              handleUriIntent(uri)
            }
          } catch (e: Exception) {
            Timber.w("Failed to parse shared text as URI: $text")
          }
        }
      }

      else -> {
        intent.data?.let { handleUriIntent(it) }
      }
    }
  }

  private fun handleUnsupportedScheme(uri: Uri) {
    Timber.w("Unsupported scheme or URI: $uri")
    // TODO: Optionally show a toast or dialog to inform user
    // Toast.makeText(context, "Unsupported link type", Toast.LENGTH_SHORT).show()
  }

  /**
   * Handle media file playback (video/audio)
   */
  @OptIn(UnstableApi::class)
  private fun handleMediaFile(uri: Uri, mediaType: String) {
    try {
      Timber.i("Opening $mediaType file: $uri")

      // Create a temporary AVPMediaItem from the URI for PlayerFragment
      val mediaItem = createMediaItemFromUri(uri, mediaType)

      // Navigate to PlayerFragment instead of PlayerActivity
      val playerFragment = PlayerFragment.newInstance(
        mediaItems = listOf(mediaItem),
        selectedMediaIdx = 0,
        subtitles = null,
        selectedSubtitleIdx = 0
      )

      mainActivity.navigate(playerFragment)
      Timber.d("Successfully navigated to PlayerFragment for $mediaType file")

    } catch (e: Exception) {
      Timber.e(e, "Failed to open $mediaType file: $uri")
      // Could show an error dialog or toast to the user
    }
  }

  /**
   * Create a temporary AVPMediaItem from URI for local files
   */
  private fun createMediaItemFromUri(uri: Uri, mediaType: String): AVPMediaItem {
    val fileName = getFileNameFromUri(uri) ?: "Unknown File"
    val fileSize = getFileSizeFromUri(uri)

    return when (mediaType) {
      "video" -> {
        // Create a VideoItem for video files using LocalVideo
        AVPMediaItem.VideoItem(
          video = cloud.app.vvf.common.models.video.Video.LocalVideo(
            id = uri.toString().hashCode().toString(),
            title = fileName,
            uri = uri.toString(),
            duration = 0L, // Will be determined by the player
            thumbnailUri = "",
            fileSize = fileSize,
            dateAdded = System.currentTimeMillis(),
            album = "Downloaded Videos",
            description = "Local video file"
          )
        )
      }
      "audio" -> {
        // Create a TrackItem for audio files
        AVPMediaItem.TrackItem(
          track = cloud.app.vvf.common.models.music.Track(
            id = uri.toString().hashCode().toString(),
            title = fileName,
            uri = uri.toString(),
            duration = null, // Will be determined by the player
            album = "Downloaded Audio",
            description = "Local audio file"
          )
        )
      }
      else -> {
        // Default to video item for unknown types using LocalVideo
        AVPMediaItem.VideoItem(
          video = cloud.app.vvf.common.models.video.Video.LocalVideo(
            id = uri.toString().hashCode().toString(),
            title = fileName,
            uri = uri.toString(),
            duration = 0L,
            thumbnailUri = "",
            fileSize = fileSize,
            dateAdded = System.currentTimeMillis(),
            album = "Downloaded Files",
            description = "Local media file"
          )
        )
      }
    }
  }

  /**
   * Extract filename from URI
   */
  private fun getFileNameFromUri(uri: Uri): String? {
    return try {
      when (uri.scheme) {
        "content" -> {
          // Try to get display name from content resolver
          mainActivity.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
          )?.use { cursor ->
            if (cursor.moveToFirst()) {
              val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
              if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } else null
          }
        }
        "file" -> {
          // Extract filename from file path
          uri.lastPathSegment
        }
        else -> {
          uri.lastPathSegment ?: "Unknown File"
        }
      } ?: "Unknown File"
    } catch (e: Exception) {
      Timber.w(e, "Failed to extract filename from URI: $uri")
      "Unknown File"
    }
  }

  /**
   * Get file size from URI
   */
  private fun getFileSizeFromUri(uri: Uri): Long {
    return try {
      when (uri.scheme) {
        "content" -> {
          mainActivity.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.SIZE),
            null,
            null,
            null
          )?.use { cursor ->
            if (cursor.moveToFirst()) {
              val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
              if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            } else 0L
          } ?: 0L
        }
        "file" -> {
          try {
            val file = java.io.File(uri.path ?: "")
            if (file.exists()) file.length() else 0L
          } catch (e: Exception) {
            0L
          }
        }
        else -> 0L
      }
    } catch (e: Exception) {
      Timber.w(e, "Failed to get file size from URI: $uri")
      0L
    }
  }

  /**
   * Check if the file path indicates a video file
   */
  private fun isVideoFile(path: String?): Boolean {
    if (path == null) return false

    val videoExtensions = setOf(
      "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
      "3gp", "3g2", "ts", "mts", "m2ts", "vob", "asf", "rm",
      "rmvb", "divx", "xvid", "f4v", "mpg", "mpeg", "m1v", "m2v"
    )

    val extension = path.substringAfterLast('.', "").lowercase()
    return extension in videoExtensions
  }

  /**
   * Check if the file path indicates an audio file
   */
  private fun isAudioFile(path: String?): Boolean {
    if (path == null) return false

    val audioExtensions = setOf(
      "mp3", "m4a", "aac", "ogg", "wav", "flac", "wma", "opus",
      "ape", "wv", "tta", "tak", "dts", "ac3", "eac3", "mka",
      "aiff", "aif", "au", "ra", "3ga", "amr", "awb"
    )

    val extension = path.substringAfterLast('.', "").lowercase()
    return extension in audioExtensions
  }
}
