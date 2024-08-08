package cloud.app.avp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import cloud.app.avp.MainActivityViewModel.Companion.isNightMode
import cloud.app.avp.databinding.ActivityMainBinding
import cloud.app.avp.viewmodels.SnackBarViewModel.Companion.configureSnackBar
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
  private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
  private val mainActivityViewModel by viewModels<MainActivityViewModel>()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(binding.root)

    enableEdgeToEdge(
      SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
      if (isNightMode()) SystemBarStyle.dark(Color.TRANSPARENT)
      else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    )

    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      mainActivityViewModel.setSystemInsets(this, insets)
      insets
    }
    val navHostFragment =
      supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
    val navController = navHostFragment.navController

    navController.addOnDestinationChangedListener { _: NavController, navDestination: NavDestination, bundle: Bundle? ->
      Timber.i(navDestination.id.toString())
    }

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if(!navController.popBackStack())
            finish()
          // if you want onBackPressed() to be called as normal afterwards

        }
      }
    )
  }
}
