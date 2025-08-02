package cloud.app.vvf.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import cloud.app.vvf.ExtensionOpenerActivity.Companion.openExtensionInstaller
import timber.log.Timber
import androidx.core.net.toUri
import cloud.app.vvf.MainActivity
import cloud.app.vvf.ui.detail.torrent.TorrentInfoFragment

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

      else -> {
        Timber.d("Handling generic content: $uri")
        handleGenericFile(uri)
      }
    }
  }

  private fun handleGenericFile(uri: Uri) {
    Timber.i("Generic file received: $uri")
    // TODO: Implement generic file handling
    // Examples:
    // - Check MIME type
    // - Handle video/audio files
    // - Handle subtitle files
    // - Show file info dialog
  }

  private fun handleMimeTypeIntent(intent: Intent, mimeType: String) {
    Timber.d("Handling MIME type: $mimeType")
    when {
      mimeType.startsWith("video/") || mimeType.startsWith("audio/") -> {
        // Handle media files
        intent.data?.let { handleGenericFile(it) }
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
}
