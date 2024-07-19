package cloud.app.avp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import cloud.app.avp.MainViewModel.Companion.isNightMode
import cloud.app.avp.databinding.ActivityMainBinding
import cloud.app.avp.utils.navigate
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
  private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
  private val mainViewModel by viewModels<MainViewModel>()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    enableEdgeToEdge(
      SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
      if (isNightMode()) SystemBarStyle.dark(Color.TRANSPARENT)
      else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    )

    val navView = binding.navView as NavigationBarView
    navView.setOnItemSelectedListener {
      //uiViewModel.navigation.value = uiViewModel.navIds.indexOf(it.itemId)
      true
    }
    navView.setOnItemReselectedListener {
      when(it.itemId) {
        R.id.homeFragment -> {
          //navigate(R.id.settingsFragment)
        }
      }
    }
    val isRail = binding.navView is NavigationRailView

  }
}
