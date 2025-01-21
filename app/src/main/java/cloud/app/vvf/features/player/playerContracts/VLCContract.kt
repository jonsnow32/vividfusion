package cloud.app.vvf.features.player.playerContracts

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import cloud.app.vvf.R
import cloud.app.vvf.features.player.PlayerContract
import cloud.app.vvf.features.player.data.PlayData
import cloud.app.vvf.features.player.data.PlayerType
import timber.log.Timber


class VLCContract(listener: PlayBackListener) : PlayerContract(listener) {
    override val packageName: String
        get() = "org.videolan.vlc"
    override val drawable: Int
        get() = R.drawable.vlc_player

    override fun getDescription(): String {
        return context.resources.getString(R.string.vlc_description)
    }

    override val canSavingPlayback: Boolean
        get() = true

    override fun getType(): String {
        return PlayerType.VLC.name
    }

    override fun handleResult(activityResult: ActivityResult) {
        if (activityResult.resultCode == Activity.RESULT_OK) {
            activityResult.data?.let {
                val positon = it.getLongExtra("extra_position", -1);
                Timber.i("Mx result with position= $positon")
                if (positon > 0)
                    //saveToDatabase(playData.entityBase, positon)
                else
                    Timber.i("too short to marked watched")
            }
        } else {
            Timber.i("MxContract return code = ${activityResult.resultCode}")
        }
    }

    override fun createIntent(context: Context, input: PlayData): Intent {
        var intent = Intent().apply {
            `package` = packageName
            setDataAndType(input.getDataUri(0), "video/*")
        }
        intent.setComponent(
            ComponentName(
                packageName,
                "org.videolan.vlc.gui.video.VideoPlayerActivity"
            )
        )
        intent.putExtra("subtitles_location", input.streamEntities[0].subtitles?.firstOrNull()?.url)
        intent.putExtra("title", input.avpMediaItem?.title)
//        intent.putExtra("position", input.entityBase?.getPosition())
        return intent
    }
}
