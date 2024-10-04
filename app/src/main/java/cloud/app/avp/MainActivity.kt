package cloud.app.avp

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import cloud.app.avp.MainActivityViewModel.Companion.isNightMode
import cloud.app.avp.databinding.ActivityMainBinding
import cloud.app.avp.features.player.PlayerManager
import cloud.app.avp.utils.Utils.isAndroidTV
import cloud.app.avp.utils.tv.screenHeight
import cloud.app.avp.utils.updateTv
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.configureSnackBar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
  private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
  private val mainActivityViewModel by viewModels<MainActivityViewModel>()
  @Inject lateinit var sharedPreferences: SharedPreferences
  override fun onCreate(savedInstanceState: Bundle?) {

    PlayerManager.getInstance().setActivityResultRegistry(activityResultRegistry)
    lifecycle.addObserver(PlayerManager.getInstance())
    super.onCreate(savedInstanceState)
    PlayerManager.getInstance()
      .inject(
        sharedPreferences,
        this
      ) // call after activity injected

    setContentView(binding.root)

    enableEdgeToEdge(
      SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
      if (isNightMode()) SystemBarStyle.dark(Color.TRANSPARENT)
      else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    )

    configureSnackBar(binding.root)

    updateTv()
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      mainActivityViewModel.setSystemInsets(this, insets)
      insets
    }

    val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
    val deviceIds = inputManager.inputDeviceIds
    val hasController = isAndroidTV(this) || isControllerConnected(inputManager, deviceIds.toList())
    //center view on focus change
    if (hasController) {
      supportFragmentManager.addOnBackStackChangedListener {
        val fragment = supportFragmentManager.fragments.lastOrNull()
        fragment?.let {
          Timber.i("Fragment ID: ${it.id}")
          // If you need the fragment tag or other properties:
          Timber.i("Fragment Tag: ${it.tag}")
        }
      }
    }
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (!supportFragmentManager.popBackStackImmediate()) {
            moveTaskToBack(true)
          }
          // if you want onBackPressed() to be called as normal afterwards
        }
      }
    )
    if(hasController) {
      binding.root.viewTreeObserver.addOnGlobalFocusChangeListener { _, view ->
        centerView(view)
      }
    }
  }


  private fun centerView(view: View?) {
    if (view == null) return
    try {
      Timber.i("centerView: $view")
      val r = Rect(0, 0, 0, 0)
      view.getDrawingRect(r)
      val x = r.centerX()
      val y = r.centerY()
      val dx = r.width() / 2
      val dy = screenHeight / 2
      val newRect = Rect(x - dx, y - dy, x + dx, y + dy)
      view.requestRectangleOnScreen(newRect, false)
      // TvFocus.current =TvFocus.current.copy(y=y.toFloat())
    } catch (_: Throwable) {
    }
  }


  fun isControllerConnected(inputManager: InputManager, deviceIds: List<Int>): Boolean {
    for (deviceId in deviceIds) {
      val device = inputManager.getInputDevice(deviceId)
      device?.let {
        if (device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
          device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        ) {
          return true
        }
      }
    }
    return false
  }
}
