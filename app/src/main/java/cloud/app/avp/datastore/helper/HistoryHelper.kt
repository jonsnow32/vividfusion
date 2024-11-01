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

fun DataStore.getHistoryData(hashCodes: Int?): Boolean {
  if (hashCodes == null) return false
  val data = getKey("$RESUME_WATCHING_FOLDER/${hashCodes}", null)
  return data != null;
}


