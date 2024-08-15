package cloud.app.avp.features.player

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import cloud.app.avp.R
import cloud.app.avp.features.player.data.PlayData
import cloud.app.avp.features.player.data.PlayerType
import cloud.app.avp.features.player.playerContracts.ExoPLayerContract
import cloud.app.avp.features.player.playerContracts.KodiContract
import cloud.app.avp.features.player.playerContracts.MxFreeContract
import cloud.app.avp.features.player.playerContracts.MxProContract
import cloud.app.avp.features.player.playerContracts.VLCContract
import cloud.app.avp.utils.Utils
import okhttp3.OkHttpClient

class PlayerManager : DefaultLifecycleObserver, PlayerContract.PlayBackListener {
  var activePlayer = PlayerType.EXO.name
  lateinit var players: List<PlayerContract>
  lateinit var registry: ActivityResultRegistry
  lateinit var owner: LifecycleOwner
  lateinit var currentFragmentManager: FragmentManager
  lateinit var context: Context
  lateinit var okHttpClient: OkHttpClient

  fun setActivityResultRegistry(registry: ActivityResultRegistry) {
    this.registry = registry
  }

  override fun onCreate(owner: LifecycleOwner) {
    if (!::registry.isInitialized) throw Exception("must be call setActivityResultRegistry(..) before activity created")
    for (player in players)
      player.register(registry, owner) //must be call before activity created
    this.owner = owner
    super.onCreate(owner)
  }

  fun inject(
    sharedPreferences: SharedPreferences,
    context: Context
  ) {
    for (player in players) {
      player.context = context
      player.sharedPreferences = sharedPreferences
    }

    activePlayer = sharedPreferences.getString("player_list", PlayerType.EXO.name).toString()
    this.context = context
  }

  fun play(playData: PlayData, fragmentManager: FragmentManager) {
    currentFragmentManager = fragmentManager
    val player = players.first { it.getType() == activePlayer }
    try {
      player.play(playData)
    } catch (e: ActivityNotFoundException) {

      showAppNotInstallDialog(player, fragmentManager)
    }
  }

  fun showAppNotInstallDialog(
    playerContract: PlayerContract,
    fragmentManager: FragmentManager
  ) {
    TODO()
//    AppNotInstallDialog.newInstance(
//      playerContract.getType().toString(),
//      playerContract.packageName,
//      playerContract.downloadUrl
//    ).show(fragmentManager, "PlayerManager")
  }

  fun activePlayer(type: String) {
    activePlayer = type
  }

  companion object {
    @SuppressLint("StaticFieldLeak")
    private lateinit var _instance: PlayerManager
    fun getInstance(): PlayerManager {
      if (!::_instance.isInitialized) {
        _instance = PlayerManager()
        _instance.players = mutableListOf<PlayerContract>().apply {
          add(MxProContract(_instance))
          add(MxFreeContract(_instance))
          add(VLCContract(_instance))
          add(ExoPLayerContract(_instance))
          add(KodiContract(_instance))
        }
      }
      return _instance
    }
  }

  override fun onPlaybackError(playerContract: PlayerContract, err: String, playData: PlayData) {
//        showAppNotInstallDialog(playerContract.getType(), playerContract.packageName, currentFragmentManager)
    var hasOtherPlayer = false
    players.filter { p -> p.getType() != playerContract.getType() }
      .forEachIndexed { index, playerContract ->
        if (Utils.isPackageExist(context, playerContract.packageName))
          try {
            playerContract.play(playData)
            hasOtherPlayer = true
            return
          } catch (e: ActivityNotFoundException) {
            showAppNotInstallDialog(
              playerContract,
              currentFragmentManager
            )
          }
      }
    if (!hasOtherPlayer) {
      if (players.size == 1) {

      }
      Toast.makeText(
        context,
        context.resources.getString(R.string.players_cannot_play),
        Toast.LENGTH_LONG
      ).show()
    }
  }
}
