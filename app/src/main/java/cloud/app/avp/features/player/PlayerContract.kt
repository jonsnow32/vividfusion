package cloud.app.avp.features.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cloud.app.avp.features.player.data.PlayData

abstract class PlayerContract(val listener: PlayBackListener) :
    ActivityResultContract<PlayData, ActivityResult>() {
    lateinit var launcher: ActivityResultLauncher<PlayData>
    lateinit var context: Context
    lateinit var playData: PlayData
    lateinit var lifecycleCoroutineScope: LifecycleCoroutineScope
    lateinit var sharedPreferences: SharedPreferences
    override fun parseResult(
        resultCode: Int, intent: Intent?
    ): ActivityResult {
        return ActivityResult(resultCode, intent)
    }

    fun register(registry: ActivityResultRegistry, lifecycle: LifecycleOwner) {
        launcher = registry.register(getType(), lifecycle, this) {
            if(::playData.isInitialized)
                handleResult(it)
        }
        lifecycleCoroutineScope = lifecycle.lifecycleScope
    }

    fun play(playData: PlayData) {
        if (!::launcher.isInitialized) throw Exception("Player hasn't register yet")
        launcher.launch(playData)
        this.playData = playData
    }

    fun handlePlaybackError(error: String) {
        listener.onPlaybackError(this, error, playData)
    }

    abstract fun getType(): String
    abstract fun handleResult(activityResult: ActivityResult)
    abstract val packageName: String
    abstract val drawable: Int
    open val downloadUrl: String? = null;
    abstract fun getDescription(): String
    abstract val canSavingPlayback: Boolean

    interface PlayBackListener {
        fun onPlaybackError(playerContract: PlayerContract, err: String, playData: PlayData);
    }
}
