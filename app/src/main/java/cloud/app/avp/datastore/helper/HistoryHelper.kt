package cloud.app.avp.datastore.helper

import cloud.app.avp.datastore.DataStore
import cloud.app.common.models.AVPMediaItem

const val RESUME_WATCHING_FOLDER = "resumewatching"

fun DataStore.addHistoryData(data: AVPMediaItem?) {
  if (data == null) return
  setKey("$RESUME_WATCHING_FOLDER/${data.id}", data)
}

fun DataStore.removeHistoryData(data: AVPMediaItem?) {
  if (data == null) return
  removeKey(
    "$RESUME_WATCHING_FOLDER/${data.id}"
  )
}

fun DataStore.getHistoryData(slug: String?): AVPMediaItem? {
  if (slug == null) return null
  val data = getKey<AVPMediaItem>("$RESUME_WATCHING_FOLDER/${slug}", null)
  return data
}


