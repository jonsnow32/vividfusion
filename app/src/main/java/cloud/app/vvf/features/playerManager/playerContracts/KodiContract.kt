package cloud.app.vvf.features.playerManager.playerContracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import cloud.app.vvf.R
import cloud.app.vvf.features.playerManager.PlayerContract
import cloud.app.vvf.features.playerManager.data.PlayData
import cloud.app.vvf.features.playerManager.data.PlayerType
import cloud.app.vvf.utils.showToast
import timber.log.Timber

open class KodiContract(listener: PlayBackListener) : PlayerContract(listener) {
  override val packageName: String
    get() = "org.xbmc.kodi"
  override val drawable: Int
    get() = R.drawable.kodi_player

  override fun getDescription(): String {
    return context.resources.getString(R.string.kodi_description)
  }

  override val canSavingPlayback: Boolean
    get() = false


  override fun getType(): String {
    return PlayerType.KODI.name
  }

  override fun handleResult(activityResult: ActivityResult) {
    if (activityResult.resultCode == Activity.RESULT_OK) {
      activityResult.data?.extras?.let {
        val positon = it.getInt("position", -1);
        val end_by = it.getString("end_by")
        val decode_mode = it.getByte("decode_mode", 0)
        Timber.i("Kodi result with position= $positon end_by= $end_by decode_mode= $decode_mode")
        if (positon > 0) {
          //saveToDatabase(playData.entityBase, positon.toLong())
        } else {
          Timber.i("too short to marked watched")
        }
      }
    } else {
      context.showToast("KODI Player can't save playback position at the moment")
      Timber.i("KODI return code = ${activityResult.resultCode}")
    }
  }


  override fun createIntent(context: Context, input: PlayData): Intent {
    val intent = Intent().apply {
      action = "android.intent.action.VIEW"
      `package` = packageName
      setDataAndType(input.getDataUri(0), "video/*")
    }
    intent.putExtra("title", input.avpMediaItem?.title)
//        intent.putExtra("position", input.entityBase?.getPosition()?.toInt())
    intent.putExtra("return_result", true)
    intent.putExtra(
      "subs",
      arrayOf(input.streamEntities[0].subtitles?.map { subtitle -> subtitle.url })
    )
    intent.putExtra(
      "subs.name",
      arrayOf(input.streamEntities[0].subtitles?.map { subtitle -> subtitle.name })
    )
    return intent
  }
}
