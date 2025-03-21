package cloud.app.vvf.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * A unified file abstraction for Android, forked from https://github.com/seven332/UniFile/tree/master
 */
abstract class KUniFile(val context: Context) {

  abstract fun createFile(displayName: String, mimeType: String = "application/octet-stream"): KUniFile?
  abstract fun createDirectory(displayName: String): KUniFile?
  abstract val uri: Uri
  abstract val name: String?
  abstract val type: String?
  abstract val filePath: String?
  abstract val isDirectory: Boolean
  abstract val isFile: Boolean
  abstract fun lastModified(): Long
  abstract fun length(): Long
  abstract fun canRead(): Boolean
  abstract fun canWrite(): Boolean
  abstract fun delete(): Boolean
  abstract fun exists(): Boolean
  abstract fun listFiles(): Array<KUniFile>?
  abstract fun findFile(displayName: String, ignoreCase: Boolean = false): KUniFile?
  abstract fun renameTo(displayName: String): Boolean
  abstract fun openInputStream(): InputStream
  abstract fun openOutputStream(append: Boolean = false): OutputStream

  companion object {
    fun fromUri(context: Context, uri: Uri): KUniFile? {
      return when (uri.scheme) {
        "file" -> {
          val path = uri.path ?: return null
          if (path.startsWith("/android_asset/")) {
            val filename = path.removePrefix("/android_asset/")
            fromAsset(context, context.assets, filename)
          } else {
            fromFile(context, File(path))
          }
        }
        "content" -> {
          when {
            DocumentsContract.isTreeUri(uri) -> {
              DocumentKUniFile(context, DocumentFile.fromTreeUri(context, uri) ?: return null, uri)
            }
            DocumentsContract.isDocumentUri(context, uri) -> {
              DocumentKUniFile(context, DocumentFile.fromSingleUri(context, uri) ?: return null, uri)
            }
            uri.authority?.contains("media") == true -> {
              val uriString = uri.toString()
              val mediaCollection = when {
                uriString.startsWith("content://media/external/downloads") -> MediaCollection.DOWNLOADS
                uriString.startsWith("content://media/external/images/media") -> MediaCollection.PICTURES
                uriString.startsWith("content://media/external/video/media") -> MediaCollection.MOVIES
                uriString.startsWith("content://media/external/audio/media") -> MediaCollection.MUSIC
                uriString.startsWith("content://media/external/file") -> MediaCollection.DOCUMENTS
                else -> null
              }
              val relativePath = uri.path?.let { path ->
                val basePath = when (mediaCollection) {
                  MediaCollection.DOWNLOADS -> "/external/downloads"
                  MediaCollection.PICTURES -> "/external/images/media"
                  MediaCollection.MOVIES -> "/external/video/media"
                  MediaCollection.MUSIC -> "/external/audio/media"
                  MediaCollection.DOCUMENTS -> "/external/file"
                  null -> null
                }
                basePath?.let { path.removePrefix(it).trimStart('/') } ?: ""
              } ?: ""
              MediaKUniFile(context, uri, relativePath, true, mediaCollection)
            }
            uri.authority == "com.android.providers.downloads.documents" -> {
              MediaKUniFile(context, uri, "", true, MediaCollection.DOCUMENTS)
            }
            else -> null
          }
        }
        "android.resource" -> {
          val resId = uri.lastPathSegment?.toIntOrNull() ?: return null
          fromResource(context, resId)
        }
        "http", "https" -> null
        else -> null
      }
    }

    fun fromFile(context: Context, file: File?): KUniFile? = file?.let { FileKUniFile(context, it) }
    fun fromAsset(context: Context, assets: AssetManager, filename: String?): KUniFile? = filename?.let { AssetKUniFile(context, assets, it) }
    fun fromResource(context: Context, resId: Int): KUniFile? = ResourceKUniFile(context, resId)

    fun fromMedia(
      context: Context,
      collection: MediaCollection = MediaCollection.DOCUMENTS,
      relativePath: String = "",
      external: Boolean = true
    ): KUniFile? {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val collectionUri = when (collection) {
          MediaCollection.DOWNLOADS -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
          MediaCollection.PICTURES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
          MediaCollection.MOVIES -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
          MediaCollection.MUSIC -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
          MediaCollection.DOCUMENTS -> MediaStore.Files.getContentUri("external")
        }
        MediaKUniFile(context, collectionUri, relativePath, external, collection)
      } else {
        val folderType = when (collection) {
          MediaCollection.DOWNLOADS -> Environment.DIRECTORY_DOWNLOADS
          MediaCollection.PICTURES -> Environment.DIRECTORY_PICTURES
          MediaCollection.MOVIES -> Environment.DIRECTORY_MOVIES
          MediaCollection.MUSIC -> Environment.DIRECTORY_MUSIC
          MediaCollection.DOCUMENTS -> Environment.DIRECTORY_DOCUMENTS
        }
        val basePath = Environment.getExternalStorageDirectory().absolutePath + File.separator + folderType
        val normalizedPath = basePath.replace(File.separator + File.separator, File.separator) +
          if (relativePath.isNotEmpty()) File.separator + relativePath else ""
        fromFile(context, File(normalizedPath))
      }
    }

    class FileKUniFile(context: Context, private val file: File) : KUniFile(context) {
      override fun createFile(displayName: String, mimeType: String): KUniFile? {
        val newFile = File(file, displayName).apply { createNewFile() }
        return if (newFile.exists()) FileKUniFile(context, newFile) else null
      }

      override fun createDirectory(displayName: String): KUniFile? {
        val newDir = File(file, displayName).apply { mkdirs() }
        return if (newDir.exists()) FileKUniFile(context, newDir) else null
      }

      override val uri: Uri get() = Uri.fromFile(file)
      override val name: String? get() = file.name.takeIf { it.isNotEmpty() } ?: "Root"
      override val type: String? get() = if (isFile) context.contentResolver.getType(uri) else null
      override val filePath: String? get() = file.absolutePath
      override val isDirectory: Boolean get() = file.isDirectory
      override val isFile: Boolean get() = file.isFile
      override fun lastModified(): Long = file.lastModified()
      override fun length(): Long = file.length()
      override fun canRead(): Boolean = file.canRead()
      override fun canWrite(): Boolean = file.canWrite()
      override fun delete(): Boolean = file.delete()
      override fun exists(): Boolean = file.exists()
      override fun listFiles(): Array<KUniFile>? = file.listFiles()?.map { FileKUniFile(context, it) }?.toTypedArray()
      override fun findFile(displayName: String, ignoreCase: Boolean): KUniFile? =
        file.listFiles()?.find { it.name.equals(displayName, ignoreCase) }?.let { FileKUniFile(context, it) }
      override fun renameTo(displayName: String): Boolean = file.renameTo(File(file.parent, displayName))
      override fun openInputStream(): InputStream = FileInputStream(file)
      override fun openOutputStream(append: Boolean): OutputStream = FileOutputStream(file, append)
    }
    class DocumentKUniFile(context: Context, private val docFile: DocumentFile, private val originalUri: Uri? = null) : KUniFile(context) {
      override fun createFile(displayName: String, mimeType: String): KUniFile? {
        if (!docFile.isDirectory) {
          throw UnsupportedOperationException("Cannot create a file inside a file. Use a directory URI instead: ${docFile.uri}")
        }
        return docFile.createFile(mimeType, displayName)?.let { DocumentKUniFile(context, it) }
      }

      override fun createDirectory(displayName: String): KUniFile? {
        if (!docFile.isDirectory) {
          throw UnsupportedOperationException("Cannot create a directory inside a file. Use a directory URI instead: ${docFile.uri}")
        }
        return docFile.createDirectory(displayName)?.let { DocumentKUniFile(context, it) }
      }

      override val uri: Uri get() = originalUri ?: docFile.uri // Use original URI if provided, else fall back to docFile.uri
      override val name: String? get() = docFile.name
      override val type: String? get() = docFile.type
      override val filePath: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val pathSegment = uri.path?.let { path ->
            when {
              path.contains("/tree/") -> path.substringAfter("/tree/").substringBefore("/document/")
              path.contains("/document/") -> path.substringAfter("/document/")
              else -> path
            }
          }?.replace("primary:", "storage/emulated/0/")?.replace(":", "/")
          pathSegment?.let { "/$it" } ?: null
        } else {
          val documentId = try {
            DocumentsContract.getDocumentId(uri)
          } catch (e: IllegalArgumentException) {
            null
          }
          documentId?.let { id ->
            if (id.startsWith("primary:")) {
              "/storage/emulated/0/${id.removePrefix("primary:")}"
            } else {
              null
            }
          } ?: uri.path
        }
      override val isDirectory: Boolean get() = docFile.isDirectory
      override val isFile: Boolean get() = docFile.isFile
      override fun lastModified(): Long = docFile.lastModified()
      override fun length(): Long = docFile.length()
      override fun canRead(): Boolean = docFile.canRead()
      override fun canWrite(): Boolean = docFile.canWrite()
      override fun delete(): Boolean = docFile.delete()
      override fun exists(): Boolean = docFile.exists()
      override fun listFiles(): Array<KUniFile>? = docFile.listFiles().map { DocumentKUniFile(context, it) }.toTypedArray()
      override fun findFile(displayName: String, ignoreCase: Boolean): KUniFile? =
        docFile.findFile(displayName)?.let { DocumentKUniFile(context, it) }
      override fun renameTo(displayName: String): Boolean = docFile.renameTo(displayName)
      override fun openInputStream(): InputStream = context.contentResolver.openInputStream(uri)!!
      override fun openOutputStream(append: Boolean): OutputStream =
        context.contentResolver.openOutputStream(uri, if (append) "wa" else "w")!!
    }
    class MediaKUniFile(
      context: Context,
      private val collection: Uri,
      private val relativePath: String = "",
      private val external: Boolean = true,
      private val mediaCollection: MediaCollection? = null
    ) : KUniFile(context) {
      private val resolver: ContentResolver = context.contentResolver
      private var cachedUri: Uri? = null

      private fun resolveUri(): Uri {
        cachedUri?.let { return it }
        val pathSegments = relativePath.split(File.separator).filter { it.isNotBlank() }
        if (pathSegments.isEmpty()) return collection

        val fileName = pathSegments.lastOrNull() ?: return collection
        val dirPath = if (pathSegments.size > 1) pathSegments.dropLast(1).joinToString(File.separator) else ""
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(dirPath, fileName)

        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            cachedUri = Uri.withAppendedPath(collection, id.toString())
            return cachedUri!!
          }
        }
        return collection
      }

      private data class MediaMetadata(
        val size: Long,
        val lastModified: Long,
        val exists: Boolean,
        val mimeType: String?,
        val displayName: String?
      )

      private fun queryMetadata(): MediaMetadata {
        val uri = resolveUri()
        if (uri == collection && cachedUri == null) return MediaMetadata(0L, 0L, false, null, null)

        val projection = arrayOf(
          MediaStore.Files.FileColumns.SIZE,
          MediaStore.Files.FileColumns.DATE_MODIFIED,
          MediaStore.Files.FileColumns.MIME_TYPE,
          MediaStore.Files.FileColumns.DISPLAY_NAME
        )
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))
            val lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)) * 1000
            val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
            return MediaMetadata(size, lastModified, true, mimeType, displayName)
          }
        }
        return if (cachedUri != null) MediaMetadata(0L, 0L, true, null, relativePath.split(File.separator).lastOrNull())
        else MediaMetadata(0L, 0L, false, null, null)
      }

      override fun createFile(displayName: String, mimeType: String): KUniFile? {
        val dirPath = relativePath.trimEnd(File.separatorChar)
        val values = ContentValues().apply {
          put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
          put(MediaStore.Files.FileColumns.RELATIVE_PATH, dirPath)
          put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
          put(MediaStore.Files.FileColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return null

        // Open output stream to ensure the file is writable
        resolver.openOutputStream(uri, "w")?.use { output ->
          // Optionally write initial content here if needed
          // e.g., output.write("".toByteArray())
        } ?: run {
          resolver.delete(uri, null, null) // Clean up if stream fails
          return null
        }

        // Finalize the file
        values.clear()
        values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
        val rowsUpdated = resolver.update(uri, values, null, null)
        if (rowsUpdated <= 0) {
          resolver.delete(uri, null, null) // Clean up if update fails
          return null
        }

        return MediaKUniFile(context, collection, "$dirPath${File.separator}$displayName", external, mediaCollection).apply { cachedUri = uri }
      }

      override fun createDirectory(displayName: String): KUniFile? {
        val newPath = "$relativePath${File.separator}$displayName".trimEnd(File.separatorChar)
        return MediaKUniFile(context, collection, newPath, external, mediaCollection)
      }

      override val uri: Uri get() = resolveUri()

      override val name: String?
        get() {
          val metadata = queryMetadata()
          if (metadata.exists) return metadata.displayName
          val pathName = relativePath.split(File.separator).filter { it.isNotBlank() }.lastOrNull()
          return pathName ?: when (mediaCollection) {
            MediaCollection.DOWNLOADS -> "Downloads"
            MediaCollection.PICTURES -> "Pictures"
            MediaCollection.MOVIES -> "Movies"
            MediaCollection.MUSIC -> "Music"
            MediaCollection.DOCUMENTS -> "Files"
            null -> "Media"
          }
        }

      override val type: String? get() = queryMetadata().mimeType
      override val filePath: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          // For API 29+, construct a pseudo-path based on the collection and relativePath
          val basePath = when (mediaCollection) {
            MediaCollection.DOWNLOADS -> "Downloads"
            MediaCollection.PICTURES -> "Pictures"
            MediaCollection.MOVIES -> "Movies"
            MediaCollection.MUSIC -> "Music"
            MediaCollection.DOCUMENTS -> "Documents"
            null -> "Media" // Fallback for unknown collections
          }
          if (relativePath.isEmpty()) "/$basePath" else "/$basePath/$relativePath"
          // Alternatively, return null if you don't want a pseudo-path:
          // null
        } else {
          "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
        }
      override val isDirectory: Boolean
        get() = relativePath.split(File.separator).filter { it.isNotBlank() }.isEmpty() || (resolveUri() == collection && cachedUri == null)
      override val isFile: Boolean get() = queryMetadata().exists
      override fun lastModified(): Long = queryMetadata().lastModified
      override fun length(): Long = queryMetadata().size
      override fun canRead(): Boolean = true
      override fun canWrite(): Boolean = true
      override fun delete(): Boolean = if (exists()) resolver.delete(uri, null, null) > 0 else false
      override fun exists(): Boolean = queryMetadata().exists

      override fun listFiles(): Array<KUniFile>? {
        if (!isDirectory) return null
        val dirPath = relativePath.trimEnd(File.separatorChar)
        val projection = arrayOf(
          MediaStore.Files.FileColumns._ID,
          MediaStore.Files.FileColumns.DISPLAY_NAME,
          MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(dirPath)
        val files = mutableListOf<KUniFile>()
        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
          while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
            val fileUri = Uri.withAppendedPath(collection, id.toString())
            files.add(MediaKUniFile(context, collection, "$dirPath${File.separator}$displayName", external, mediaCollection)
              .apply { cachedUri = fileUri })
          }
        }
        return files.toTypedArray()
      }

      override fun findFile(displayName: String, ignoreCase: Boolean): KUniFile? {
        if (!isDirectory) return null
        val dirPath = relativePath.trimEnd(File.separatorChar)
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(dirPath, displayName)
        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            val fileUri = Uri.withAppendedPath(collection, id.toString())
            return MediaKUniFile(context, collection, "$dirPath${File.separator}$displayName", external, mediaCollection)
              .apply { cachedUri = fileUri }
          }
        }
        return null
      }

      override fun renameTo(displayName: String): Boolean {
        if (!exists()) return false
        val values = ContentValues().apply {
          put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
        }
        val success = resolver.update(uri, values, null, null) > 0
        if (success) cachedUri = null
        return success
      }

      override fun openInputStream(): InputStream {
        if (!isFile) throw IllegalStateException("Cannot open input stream for a directory: $relativePath")
        return resolver.openInputStream(uri)!!
      }

      override fun openOutputStream(append: Boolean): OutputStream {
        if (isDirectory) throw IllegalStateException("Cannot open output stream for a directory: $relativePath")
        if (!exists()) throw IllegalStateException("File does not exist yet. Call createFile() first: $relativePath")
        return resolver.openOutputStream(uri, if (append) "wa" else "w")!!
      }
    }

    class AssetKUniFile(context: Context, private val assets: AssetManager, private val filename: String) : KUniFile(context) {
      override fun createFile(displayName: String, mimeType: String): KUniFile? = null
      override fun createDirectory(displayName: String): KUniFile? = null
      override val uri: Uri get() = Uri.parse("file:///android_asset/$filename")
      override val name: String? get() = filename.substringAfterLast("/")
      override val type: String? get() = context.contentResolver.getType(uri) ?: "application/octet-stream"
      override val filePath: String? get() = null
      override val isDirectory: Boolean get() = false
      override val isFile: Boolean get() = true
      override fun lastModified(): Long = 0L
      override fun length(): Long = assets.openFd(filename).length
      override fun canRead(): Boolean = true
      override fun canWrite(): Boolean = false
      override fun delete(): Boolean = false
      override fun exists(): Boolean = true
      override fun listFiles(): Array<KUniFile>? = null
      override fun findFile(displayName: String, ignoreCase: Boolean): KUniFile? = null
      override fun renameTo(displayName: String): Boolean = false
      override fun openInputStream(): InputStream = assets.open(filename)
      override fun openOutputStream(append: Boolean): OutputStream =
        throw UnsupportedOperationException("Assets are read-only")
    }

    class ResourceKUniFile(context: Context, private val resId: Int) : KUniFile(context) {
      override fun createFile(displayName: String, mimeType: String): KUniFile? = null
      override fun createDirectory(displayName: String): KUniFile? = null
      override val uri: Uri get() = Uri.parse("android.resource://${context.packageName}/$resId")
      override val name: String? get() = context.resources.getResourceEntryName(resId)
      override val type: String? get() = context.contentResolver.getType(uri) ?: "application/octet-stream"
      override val filePath: String? get() = null
      override val isDirectory: Boolean get() = false
      override val isFile: Boolean get() = true
      override fun lastModified(): Long = 0L
      override fun length(): Long = 0L
      override fun canRead(): Boolean = true
      override fun canWrite(): Boolean = false
      override fun delete(): Boolean = false
      override fun exists(): Boolean = true
      override fun listFiles(): Array<KUniFile>? = null
      override fun findFile(displayName: String, ignoreCase: Boolean): KUniFile? = null
      override fun renameTo(displayName: String): Boolean = false
      override fun openInputStream(): InputStream = context.resources.openRawResource(resId)
      override fun openOutputStream(append: Boolean): OutputStream =
        throw UnsupportedOperationException("Resources are read-only")
    }

    enum class MediaCollection {
      DOWNLOADS,
      PICTURES,
      MOVIES,
      MUSIC,
      DOCUMENTS
    }
  }
}
