package cloud.app.avp.viewmodels

import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.avp.ExceptionActivity
import cloud.app.avp.R
import cloud.app.avp.ui.exception.ExceptionFragment.Companion.getTitle
import cloud.app.avp.utils.observe
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnackBarViewModel @Inject constructor(
  mutableThrowableFlow: MutableSharedFlow<Throwable>,
  val mutableMessageFlow: MutableSharedFlow<Message>
) : ViewModel() {

  val throwableFlow = mutableThrowableFlow.asSharedFlow()

  data class Message(
    val message: String,
    val action: Action? = null,
    val anchorView: View? = null
  )

  data class Action(
    val name: String,
    val handler: (() -> Unit)
  )

  private val messages = mutableListOf<Message>()

  fun create(message: Message) {
    if (messages.isEmpty()) viewModelScope.launch {
      mutableMessageFlow.emit(message)
    }
    if (!messages.contains(message)) messages.add(message)
  }

  fun remove(message: Message, dismissed: Boolean) {
    if (dismissed) messages.remove(message)
    if (messages.isNotEmpty()) viewModelScope.launch {
      mutableMessageFlow.emit(messages.first())
    }
  }

  companion object {
    fun AppCompatActivity.configureSnackBar(anchorView: View) {
      val viewModel by viewModels<SnackBarViewModel>()
      fun createSnackBar(message: Message) {
        val snackBar = Snackbar.make(
          anchorView,
          message.message,
          Snackbar.LENGTH_LONG
        )
        snackBar.animationMode = Snackbar.ANIMATION_MODE_SLIDE
        snackBar.view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = 0}
        //if (anchorView !is NavigationRailView) snackBar.anchorView = anchorView
        message.action?.run { snackBar.setAction(name) { handler() } }
        snackBar.addCallback(object : Snackbar.Callback() {
          override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            viewModel.remove(message, event != DISMISS_EVENT_MANUAL)
          }
        })
        snackBar.show()
      }

      observe(viewModel.mutableMessageFlow) { message ->
        createSnackBar(message)
      }

      observe(viewModel.throwableFlow) { throwable ->
        throwable.printStackTrace()
        val message = Message(
          message = getTitle(throwable),
          action = Action(getString(R.string.view)) {
            ExceptionActivity.start(this, throwable)
          }
        )
        viewModel.create(message)
      }
    }

    fun Fragment.createSnack(message: Message) {
      val viewModel by activityViewModels<SnackBarViewModel>()
      viewModel.create(message)
    }

    fun Fragment.createSnack(message: String) {
      createSnack(Message(message))
    }

    fun Fragment.createSnack(message: Int) {
      createSnack(getString(message))
    }
  }
}
