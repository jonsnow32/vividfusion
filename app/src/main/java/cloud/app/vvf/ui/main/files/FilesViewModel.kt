package cloud.app.vvf.ui.main.files

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.video.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor() : ViewModel() {

  private val _files = MutableStateFlow<List<AVPMediaItem.VideoItem>>(emptyList())
  val files: StateFlow<List<AVPMediaItem.VideoItem>> = _files

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading

  fun loadFiles(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      _files.value = queryLocalVideos(context)
      _isLoading.value = false
    }
  }

  private fun queryLocalVideos(context: Context): List<AVPMediaItem.VideoItem> {
    val results = mutableListOf<AVPMediaItem.VideoItem>()
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DISPLAY_NAME,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.DATE_ADDED,
      MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
    )
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

    context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
      val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
      val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

      while (cursor.moveToNext()) {
        val id = cursor.getLong(idCol)
        val name = cursor.getString(nameCol) ?: continue
        val duration = cursor.getLong(durationCol)
        val size = cursor.getLong(sizeCol)
        val dateAdded = cursor.getLong(dateCol)
        val bucket = cursor.getString(bucketCol) ?: ""
        val contentUri = ContentUris.withAppendedId(collection, id)

        val video = Video.LocalVideo(
          uri = contentUri.toString(),
          title = name.substringBeforeLast("."),
          duration = duration,
          thumbnailUri = contentUri.toString(),
          id = id.toString(),
          fileSize = size,
          dateAdded = dateAdded,
          album = bucket,
        )
        results.add(AVPMediaItem.VideoItem(video))
      }
    }
    return results
  }
}
