package cloud.app.vvf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.commit
import cloud.app.vvf.VVFApplication.Companion.restartApp
import cloud.app.vvf.databinding.ActivityExceptionBinding
import cloud.app.vvf.ui.exception.ExceptionFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExceptionActivity : AppCompatActivity() {

  private val binding by lazy { ActivityExceptionBinding.inflate(layoutInflater) }
  private val exception: String by lazy { intent.getStringExtra(EXTRA_STACKTRACE)!! }
  private val mainActivityViewModel by viewModels<MainActivityViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    enableEdgeToEdge()
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
      mainActivityViewModel.setSystemInsets(this, insets)
      //binding.fabContainer.applyInsets(mainViewModel.systemInsets.value)
      insets
    }
    supportFragmentManager.commit {
      replace(
        R.id.exceptionFragmentContainer,
        ExceptionFragment.newInstance(AppCrashException(exception))
      )
    }
    binding.restartApp.setOnClickListener { restartApp() }
  }

  companion object {
    const val EXTRA_STACKTRACE = "stackTrace"
    fun start(context: Context, exception: Throwable, isCached: Boolean = true) {
      val intent = Intent(context, ExceptionActivity::class.java).apply {
        putExtra(EXTRA_STACKTRACE, exception.stackTraceToString())
        if(!isCached)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
      context.startActivity(intent)
    }
  }

  class AppCrashException(val causedBy: String) : Exception()
}
