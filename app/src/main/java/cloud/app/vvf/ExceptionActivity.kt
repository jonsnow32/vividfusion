package cloud.app.vvf

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.commit
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.VVFApplication.Companion.restartApp
import cloud.app.vvf.databinding.ActivityExceptionBinding
import cloud.app.vvf.ui.exception.ExceptionFragment
import cloud.app.vvf.ui.exception.ExceptionFragment.Companion.getDetails
import cloud.app.vvf.ui.exception.ExceptionFragment.Companion.getTitle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class ExceptionActivity : AppCompatActivity() {

  private val binding by lazy { ActivityExceptionBinding.inflate(layoutInflater) }
  private val stackTrace: String by lazy { intent.getStringExtra(EXTRA_STACKTRACE)!! }
  private val title: String by lazy { intent.getStringExtra(EXTRA_TITLE)!! }
  private val mainActivityViewModel by viewModels<MainActivityViewModel>()

  @Inject
  lateinit var sharedPreferences: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    enableEdgeToEdge()
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      mainActivityViewModel.setSystemInsets(this, insets)
      insets
    }


    supportFragmentManager.commit {
      replace(
        R.id.exceptionFragmentContainer,
        ExceptionFragment.newInstance(
          ExceptionDetails(
            title,
            stackTrace
          )
        )
      )
    }
    binding.restartApp.setOnClickListener { restartApp() }
  }

  companion object {
    const val EXTRA_STACKTRACE = "stackTrace"
    const val EXTRA_TITLE = "title"
    fun start(context: Context, exception: Throwable, forceClose: Boolean = false) {
      val intent = Intent(context, ExceptionActivity::class.java).apply {
        putExtra(EXTRA_STACKTRACE, context.getDetails(exception))
        putExtra(EXTRA_TITLE, context.getTitle(exception))
        if(forceClose) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
      context.startActivity(intent)
    }
  }

  @Serializable
  class ExceptionDetails(val title: String, val causedBy: String)
}
