package cloud.app.vvf.features.player.playerContracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import cloud.app.vvf.BuildConfig
import cloud.app.vvf.R
import cloud.app.vvf.features.player.PlayerContract
import cloud.app.vvf.features.player.data.PlayData
import cloud.app.vvf.features.player.data.PlayerType
import timber.log.Timber


class ExoPLayerContract(listener: PlayBackListener) : PlayerContract(listener) {
  override val packageName: String
    get() = "cloud.app.csplayer" + if (BuildConfig.DEBUG) ".debug" else ""
  override val drawable: Int
    get() = R.drawable.exo_player

  override val downloadUrl: String
    get() = "https://github.com/staronecloud/csplayer-release/releases/download/pre-release/app-debug.apk";

  override fun getDescription(): String {
    return context.resources.getString(R.string.exo_description)
  }

  override val canSavingPlayback: Boolean
    get() = true

  override fun getType(): String {
    return PlayerType.EXO.name
  }

  override fun handleResult(activityResult: ActivityResult) {
    if (activityResult.resultCode == Activity.RESULT_OK) {
      activityResult.data?.extras?.let {
        val positon = it.getLong("position", -1);
        val end_by = it.getString("end_by")
        val decode_mode = it.getByte("decode_mode", 0)
        Timber.i("Exo result with position= $positon end_by= $end_by decode_mode= $decode_mode")
        if (positon > 0) {
          //saveToDatabase(playData.entityBase, positon)
        } else {
          Timber.i("too short to marked watched")
        }
      }
    } else {
      if (activityResult.resultCode == Activity.RESULT_CANCELED) {
        activityResult.data?.extras?.let {
          val endBy = it.getString("end_by", "")
          if (endBy.equals("video_codecs_not_support") || endBy.equals("audio_codecs_not_support")) {
            handlePlaybackError(endBy);
          }
        }
      }
    }

    Timber.i("ExoContract return code = ${activityResult.resultCode}")
  }

  override fun createIntent(context: Context, input: PlayData): Intent {

    val intent = Intent().apply {
      action = "android.intent.action.VIEW"
      `package` = packageName
      setDataAndType(input.getDataUri(input.selectedId), "video/*")
    }

    intent.putExtra(EXTRA_TITLE, input.avpMediaItem?.title)
//        intent.putExtra(EXTRA_POSITION, input.avpMediaItem?.getPosition())

    val videosHeadersArrays = mutableListOf<String>()
    input.streamEntities.forEach { streamEntity ->
      streamEntity.resolvedUrl?.let {
        videosHeadersArrays.add(it)
        videosHeadersArrays.add(streamEntity.toString())
        val headers = mutableListOf<String>()
        streamEntity.headers?.map { header ->
          headers.add(header.key)
          headers.add(header.value)
        }
        videosHeadersArrays.add(headers.joinToString((".|.")))
      }
    }
    intent.putExtra(EXTRA_VIDEO_URLS_NAME_HEADERS, videosHeadersArrays.toTypedArray())
    intent.putExtra(EXTRA_VIDEO_START_INDEX, input.selectedId)
    val subtitlesArray = mutableListOf<String>()
    input.streamEntities[input.selectedId].subtitles?.map { subtitle ->
      if (subtitle.languageCode != null) {
        subtitlesArray.add(subtitle.languageCode!!)
        subtitlesArray.add(subtitle.url)
      }
    }

    intent.putExtra(EXTRA_SUBTITLE_LIST, subtitlesArray.toTypedArray())
    intent.putExtra(EXTRA_SUBTITLE_START_INDEX, 0)
    intent.putExtra(EXTRA_HAS_AD, input.needToShowAd)

    return intent
  }

  companion object {
    const val EXTRA_TITLE: String = "title"
    const val EXTRA_POSITION: String = "position"
    const val EXTRA_VIDEO_URLS_NAME_HEADERS: String = "video_url_headers"
    const val EXTRA_VIDEO_START_INDEX: String = "video_start_index"
    const val EXTRA_IS_SAME_EPISODE = "is_same_episode" // boolean
    const val EXTRA_SUBTITLE_LIST: String = "subtitles" // string[]
    const val EXTRA_SUBTITLE_START_INDEX: String = "subtitle_start_index" //int
    const val EXTRA_HAS_AD: String = "has_ad" // boolean
  }
}
