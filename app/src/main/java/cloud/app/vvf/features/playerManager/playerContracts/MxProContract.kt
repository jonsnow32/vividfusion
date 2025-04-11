package cloud.app.vvf.features.playerManager.playerContracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import cloud.app.vvf.R
import cloud.app.vvf.features.playerManager.PlayerContract
import cloud.app.vvf.features.playerManager.data.PlayData
import cloud.app.vvf.features.playerManager.data.PlayerType
import timber.log.Timber

open class MxProContract(listener: PlayBackListener) : PlayerContract(listener) {
    override val packageName: String
        get() = "com.mxtech.videoplayer.pro"
    override val drawable: Int
        get() = R.drawable.mx_player

    override fun getDescription(): String {
        return context.resources.getString(R.string.mx_description)
    }

    override val canSavingPlayback: Boolean
        get() = true


    override fun getType(): String {
        return PlayerType.MXPro.name
    }

    override fun handleResult(activityResult: ActivityResult) {
        if (activityResult.resultCode == Activity.RESULT_OK) {
            activityResult.data?.extras?.let {
                val positon = it.getInt("position", -1);
                val end_by = it.getString("end_by")
                val decode_mode = it.getByte("decode_mode", 0)
                Timber.i("Mx result with position= $positon end_by= $end_by decode_mode= $decode_mode")
                if (positon > 0) {
                    //saveToDatabase(playData.entityBase, positon.toLong())
                } else {
                    Timber.i("too short to marked watched")
                }
            }
        } else {
            Timber.i("MxContract return code = ${activityResult.resultCode}")
        }
    }


    override fun createIntent(context: Context, input: PlayData): Intent {
        val intent = Intent().apply {
            action = "android.intent.action.VIEW"
            `package` = packageName
            setDataAndType(input.getDataUri(0), "video/*")
        }
        intent.putExtra("title", input.avpMediaItem?.title)
//        intent.putExtra("position", input.avpMediaItem?.getPosition()?.toInt())
        intent.putExtra("return_result", true)
        intent.putExtra("subs", arrayOf(input.streamEntities[0].subtitles?.map { subtitle -> subtitle.url }))
        intent.putExtra("subs.name", arrayOf(input.streamEntities[0].subtitles?.map { subtitle -> subtitle.name }))

        val headers = mutableListOf<String>()
        input.streamEntities[0].headers?.map { it ->
            headers.add(it.key)
            headers.add(it.value)
        }
        intent.putExtra("headers", headers.toTypedArray())
        return intent
    }
}
