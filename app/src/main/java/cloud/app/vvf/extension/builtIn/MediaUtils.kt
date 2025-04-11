package cloud.app.vvf.extension.builtIn

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import cloud.app.vvf.common.exceptions.AppPermissionRequiredException
import cloud.app.vvf.common.models.movie.LocalAlbum
import cloud.app.vvf.common.models.movie.LocalVideo
import timber.log.Timber
import java.util.Calendar

object MediaUtils {

  val className = MediaUtils.javaClass.toString()
  private val videoProjection = arrayOf(
    MediaStore.Video.Media._ID,
    MediaStore.Video.Media.DISPLAY_NAME,
    MediaStore.Video.Media.DATA,
    MediaStore.Video.Media.DURATION,
    MediaStore.Video.Media.SIZE,
    MediaStore.Video.Media.DATE_ADDED,
    MediaStore.Video.Media.ALBUM,
    MediaStore.Video.Media.WIDTH,
    MediaStore.Video.Media.HEIGHT,
    MediaStore.Video.Media.IS_PENDING
  )

  fun hasVideoPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun getAllVideos(context: Context, page: Int = 1, pageSize: Int = 20): List<LocalVideo> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }

    if (!hasVideoPermission(context)) {
      Timber.e("Missing video permissions!")
      throw AppPermissionRequiredException(className, "BuitInClient", Manifest.permission.READ_MEDIA_VIDEO)
    }

    val videos = mutableListOf<LocalVideo>()
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val selection = "${MediaStore.Video.Media.IS_PENDING} = 0"
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    context.contentResolver.query(uri, videoProjection, selection, null, sortOrder)?.use { cursor ->
      val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
      val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
      val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
      val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
      val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
      val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

      val startPosition = (page - 1) * pageSize
      val endPosition = minOf(startPosition + pageSize, cursor.count)

      if (startPosition >= cursor.count) {
        return emptyList()
      }

      if (cursor.moveToPosition(startPosition)) {
        var currentPosition = startPosition
        do {
          val id = cursor.getStringOrNull(idColumn) ?: continue
          val title = cursor.getStringOrNull(titleColumn) ?: "Unknown Title"
          val data = cursor.getStringOrNull(dataColumn) ?: continue
          val duration = cursor.getLongOrNull(durationColumn) ?: 0L
          val size = cursor.getLongOrNull(sizeColumn) ?: 0L
          val dateAdded = cursor.getLongOrNull(dateAddedColumn)?.times(1000) ?: 0L
          val album = cursor.getStringOrNull(albumColumn) ?: "Unknown Album"
          val width = cursor.getIntOrNull(widthColumn)
          val height = cursor.getIntOrNull(heightColumn)

          val thumbnailUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            id.toLong()
          ).toString()

          videos.add(
            LocalVideo(
              id = id,
              title = title,
              poster = thumbnailUri,
              uri = data,
              duration = duration,
              size = size,
              dateAdded = dateAdded,
              album = album,
              width = width,
              height = height
            )
          )
          currentPosition++
        } while (currentPosition < endPosition && cursor.moveToNext())
      }
    }

    return videos
  }

  fun getAllAlbums(context: Context): List<LocalAlbum> {
    val videos = mutableListOf<LocalVideo>()
    var page = 1
    val pageSize = 50

    while (true) {
      val pageVideos = getAllVideos(context, page, pageSize)
      if (pageVideos.isEmpty()) break
      videos.addAll(pageVideos)
      page++
    }

    val albumMap = mutableMapOf<String, MutableList<LocalVideo>>()
    videos.forEach { video ->
      albumMap.getOrPut(video.album) { mutableListOf() }.add(video)
    }

    return albumMap.map { (albumName, videoList) ->
      val totalDuration = videoList.sumOf { it.duration }
      val firstVideo = videoList.first()
      LocalAlbum(
        id = albumName.hashCode().toString(),
        title = albumName,
        poster = firstVideo.poster,
        uri = firstVideo.uri,
        duration = totalDuration,
        videos = videoList
      )
    }.sortedBy { it.title }
  }

  fun refreshMediaStore(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      context.contentResolver.query(
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        arrayOf(MediaStore.Video.Media._ID),
        null,
        null,
        null
      )?.close()
    } else {
      context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE))
    }
  }

  fun getVideoByRangeOfDays(
    context: Context,
    from: Long,
    to: Long,
    page: Int = 1,
    pageSize: Int = 20
  ): List<LocalVideo> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(from <= to) { "From date must be less than or equal to To date" }

    if (!hasVideoPermission(context)) {
      Timber.e("Missing video permissions!")
      throw AppPermissionRequiredException(className, "BuitInClient", Manifest.permission.READ_MEDIA_VIDEO)
    }

    refreshMediaStore(context)

    val videos = mutableListOf<LocalVideo>()
    val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      listOf(
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_INTERNAL)
      )
    } else {
      listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    }

    uris.forEach { uri ->
      val selection = "${MediaStore.Video.Media.DATE_ADDED} >= ? AND ${MediaStore.Video.Media.DATE_ADDED} <= ?"
      val selectionArgs = arrayOf(
        (from / 1000).toString(),
        (to / 1000).toString()
      )
      val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

      Timber.d("Querying URI: $uri, From: $from, To: $to")

      context.contentResolver.query(uri, videoProjection, selection, selectionArgs, sortOrder)?.use { cursor ->
        Timber.d("Cursor count for $uri: ${cursor.count}")
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
        val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
        val isPendingColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_PENDING)

        val startPosition = (page - 1) * pageSize
        val endPosition = minOf(startPosition + pageSize, cursor.count)

        if (startPosition >= cursor.count) {
          Timber.d("No videos in range for page $page")
          return@use
        }

        if (cursor.moveToPosition(startPosition)) {
          var currentPosition = startPosition
          do {
            val id = cursor.getStringOrNull(idColumn) ?: continue
            val title = cursor.getStringOrNull(titleColumn) ?: "Unknown Title"
            val data = cursor.getStringOrNull(dataColumn) ?: continue
            val duration = cursor.getLongOrNull(durationColumn) ?: 0L
            val size = cursor.getLongOrNull(sizeColumn) ?: 0L
            val dateAdded = cursor.getLongOrNull(dateAddedColumn)?.times(1000) ?: 0L
            val album = cursor.getStringOrNull(albumColumn) ?: "Unknown Album"
            val width = cursor.getIntOrNull(widthColumn)
            val height = cursor.getIntOrNull(heightColumn)

            val isPending = cursor.getInt(isPendingColumn) == 1

            val thumbnailUri = ContentUris.withAppendedId(
              MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
              id.toLong()
            ).toString()

            Timber.d("Video: $title, Date: $dateAdded, Pending: $isPending, Thumbnail: $thumbnailUri")

            videos.add(
              LocalVideo(
                id = id,
                title = title,
                poster = thumbnailUri,
                uri = data,
                duration = duration,
                size = size,
                dateAdded = dateAdded,
                album = album,
                width = width,
                height = height
              )
            )
            currentPosition++
          } while (currentPosition < endPosition && cursor.moveToNext())
        }
      } ?: Timber.w("Query returned null cursor for URI: $uri")
    }

    return videos
  }

  fun getVideoCount(context: Context): Int {
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val selection = "${MediaStore.Video.Media.IS_PENDING} = 0"

    context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media._ID), selection, null, null)?.use { cursor ->
      return cursor.count
    }
    return 0
  }

  private val minimalProjection = arrayOf(MediaStore.Video.Media.DATE_ADDED)

  fun getOldestVideoYear(context: Context): Int? {
    if (!hasVideoPermission(context)) {
      Timber.e("Missing video permissions!")
      throw AppPermissionRequiredException(className, "BuitInClient", Manifest.permission.READ_MEDIA_VIDEO)
    }

    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val selection = "${MediaStore.Video.Media.IS_PENDING} = 0"
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} ASC"

    var oldestYear: Int? = null

    context.contentResolver.query(uri, minimalProjection, selection, null, sortOrder)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val oldestDateAdded = cursor.getLongOrNull(dateAddedColumn)?.times(1000) ?: return null
        val calendar = Calendar.getInstance().apply { timeInMillis = oldestDateAdded }
        oldestYear = calendar.get(Calendar.YEAR)
      }
    } ?: Timber.w("Query returned null cursor when finding oldest video year")

    return oldestYear
  }

  fun getVideosByAlbum(
    context: Context,
    albumName: String,
    page: Int = 1,
    pageSize: Int = 20
  ): List<LocalVideo> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(albumName.isNotEmpty()) { "Album name cannot be empty" }

    if (!hasVideoPermission(context)) {
      Timber.e("Missing video permissions!")
      throw AppPermissionRequiredException(className, "BuitInClient", Manifest.permission.READ_MEDIA_VIDEO)
    }

    val videos = mutableListOf<LocalVideo>()
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val selection = "${MediaStore.Video.Media.ALBUM} = ? AND ${MediaStore.Video.Media.IS_PENDING} = 0"
    val selectionArgs = arrayOf(albumName)
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    context.contentResolver.query(uri, videoProjection, selection, selectionArgs, sortOrder)?.use { cursor ->
      val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
      val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
      val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
      val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
      val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
      val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

      val startPosition = (page - 1) * pageSize
      val endPosition = minOf(startPosition + pageSize, cursor.count)

      if (startPosition >= cursor.count) {
        return emptyList()
      }

      if (cursor.moveToPosition(startPosition)) {
        var currentPosition = startPosition
        do {
          val id = cursor.getStringOrNull(idColumn) ?: continue
          val title = cursor.getStringOrNull(titleColumn) ?: "Unknown Title"
          val data = cursor.getStringOrNull(dataColumn) ?: continue
          val duration = cursor.getLongOrNull(durationColumn) ?: 0L
          val size = cursor.getLongOrNull(sizeColumn) ?: 0L
          val dateAdded = cursor.getLongOrNull(dateAddedColumn)?.times(1000) ?: 0L
          val album = cursor.getStringOrNull(albumColumn) ?: "Unknown Album"
          val width = cursor.getIntOrNull(widthColumn)
          val height = cursor.getIntOrNull(heightColumn)

          val thumbnailUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            id.toLong()
          ).toString()

          videos.add(
            LocalVideo(
              id = id,
              title = title,
              poster = thumbnailUri,
              uri = data,
              duration = duration,
              size = size,
              dateAdded = dateAdded,
              album = album,
              width = width,
              height = height
            )
          )
          currentPosition++
        } while (currentPosition < endPosition && cursor.moveToNext())
      }
    }

    return videos
  }

  fun getAllVideosByAlbum(context: Context, albumName: String): List<LocalVideo> {
    require(albumName.isNotEmpty()) { "Album name cannot be empty" }

    if (!hasVideoPermission(context)) {
      Timber.e("Missing video permissions!")
      throw AppPermissionRequiredException(className, "BuitInClient", Manifest.permission.READ_MEDIA_VIDEO)
    }

    val videos = mutableListOf<LocalVideo>()
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    val selection = "${MediaStore.Video.Media.ALBUM} = ? AND ${MediaStore.Video.Media.IS_PENDING} = 0"
    val selectionArgs = arrayOf(albumName)
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    context.contentResolver.query(uri, videoProjection, selection, selectionArgs, sortOrder)?.use { cursor ->
      val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
      val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
      val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
      val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
      val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
      val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

      while (cursor.moveToNext()) {
        val id = cursor.getStringOrNull(idColumn) ?: continue
        val title = cursor.getStringOrNull(titleColumn) ?: "Unknown Title"
        val data = cursor.getStringOrNull(dataColumn) ?: continue
        val duration = cursor.getLongOrNull(durationColumn) ?: 0L
        val size = cursor.getLongOrNull(sizeColumn) ?: 0L
        val dateAdded = cursor.getLongOrNull(dateAddedColumn)?.times(1000) ?: 0L
        val album = cursor.getStringOrNull(albumColumn) ?: "Unknown Album"
        val width = cursor.getIntOrNull(widthColumn)
        val height = cursor.getIntOrNull(heightColumn)

        val thumbnailUri = ContentUris.withAppendedId(
          MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
          id.toLong()
        ).toString()

        videos.add(
          LocalVideo(
            id = id,
            title = title,
            poster = thumbnailUri,
            uri = data,
            duration = duration,
            size = size,
            dateAdded = dateAdded,
            album = album,
            width = width,
            height = height
          )
        )
      }
    }

    return videos
  }
}
