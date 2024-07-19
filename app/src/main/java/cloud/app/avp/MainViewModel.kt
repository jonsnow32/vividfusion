package cloud.app.avp

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.os.Bundle
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import cloud.app.avp.utils.dpToPx
import cloud.app.avp.utils.observe
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val settings: SharedPreferences,
) : ViewModel() {

  data class Insets(
    val top: Int = 0,
    val bottom: Int = 0,
    val start: Int = 0,
    val end: Int = 0
  ) {
    fun add(vararg insets: Insets) = insets.fold(this) { acc, it ->
      Insets(
        acc.top + it.top,
        acc.bottom + it.bottom,
        acc.start + it.start,
        acc.end + it.end
      )
    }
  }

  val navigation = MutableStateFlow(0)
  val navigationReselected = MutableSharedFlow<Int>()
  val navIds = listOf(
    R.id.homeFragment,
    R.id.searchFragment,
    R.id.libraryFragment
  )
  private val navViewInsets = MutableStateFlow(Insets())
  private val playerNavViewInsets = MutableStateFlow(Insets())
  private val playerInsets = MutableStateFlow(Insets())
  val systemInsets = MutableStateFlow(Insets())
  var isMainFragment = MutableStateFlow(true)

  val combined = systemInsets.combine(navViewInsets) { system, nav ->
    if (isMainFragment.value) system.add(nav) else system
  }.combine(playerInsets) { system, player ->
    system.add(player)
  }

  private fun backPressCallback() = object : OnBackPressedCallback(false) {
    override fun handleOnBackStarted(backEvent: BackEventCompat) {

    }

    override fun handleOnBackProgressed(backEvent: BackEventCompat) {

    }

    override fun handleOnBackPressed() {

    }

    override fun handleOnBackCancelled() {

    }
  }

  fun Fragment.applyBackPressCallback(callback: ((Int) -> Unit)? = null) {
    val activity = requireActivity()
    val viewModel by activity.viewModels<MainViewModel>()
    val backPress = viewModel.backPressCallback()
    activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPress)
  }

  fun setPlayerNavViewInsets(context: Context, isNavVisible: Boolean, isRail: Boolean): Insets {
    val insets = context.resources.run {
      if (!isNavVisible) return@run Insets()
      val height = getDimensionPixelSize(R.dimen.nav_height)
      if (!isRail) return@run Insets(bottom = height)
      val width = getDimensionPixelSize(R.dimen.nav_width)
      if (context.isRTL()) Insets(end = width) else Insets(start = width)
    }
    playerNavViewInsets.value = insets
    return insets
  }

  fun setNavInsets(insets: Insets) {
    navViewInsets.value = insets
  }

  fun setSystemInsets(context: Context, insets: WindowInsetsCompat) {
    val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val inset = system.run {
      if (context.isRTL()) Insets(top, bottom, right, left)
      else Insets(top, bottom, left, right)
    }
    systemInsets.value = inset
  }

  fun setPlayerInsets(context: Context, isVisible: Boolean) {
    val insets = if (isVisible) {
      val height = context.resources.getDimensionPixelSize(R.dimen.collapsed_cover_size)
      Insets(bottom = height)
    } else Insets()
    playerInsets.value = insets
  }


  companion object {
    fun Context.isRTL() =
      resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    fun Context.isLandscape() =
      resources.configuration.orientation == ORIENTATION_LANDSCAPE

    fun Context.isNightMode() =
      resources.configuration.uiMode and UI_MODE_NIGHT_MASK != UI_MODE_NIGHT_NO

    fun Fragment.applyInsets(block: MainViewModel.(Insets) -> Unit) {
      val mainViewModel by activityViewModels<MainViewModel>()
      observe(mainViewModel.combined) { mainViewModel.block(it) }
    }

    fun Fragment.applyInsetsMain(
      appBar: View,
      child: View,
      block: MainViewModel.(Insets) -> Unit = {}
    ) {
      val mainViewModel by activityViewModels<MainViewModel>()
      observe(mainViewModel.combined) { insets ->
        child.applyContentInsets(insets)
        appBar.updatePaddingRelative(
          top = insets.top,
          start = insets.start,
          end = insets.end
        )

        mainViewModel.block(insets)
      }
    }

    fun View.applyContentInsets(insets: Insets, paddingDp: Int = 8) {
      val verticalPadding = paddingDp.dpToPx(context)
      updatePaddingRelative(
        top = verticalPadding,
        bottom = insets.bottom + verticalPadding,
        start = insets.start,
        end = insets.end
      )
    }

    fun View.applyInsets(it: Insets, paddingDp: Int = 0) {
      val padding = paddingDp.dpToPx(context)
      updatePaddingRelative(
        top = it.top + padding,
        bottom = it.bottom + padding,
        start = it.start + padding,
        end = it.end + padding,
      )
    }

    fun View.applyFabInsets(it: Insets, system: Insets, paddingDp: Int = 0) {
      val padding = paddingDp.dpToPx(context)
      updatePaddingRelative(
        bottom = it.bottom - system.bottom + padding,
        start = it.start + padding,
        end = it.end + padding,
      )
    }

    fun Fragment.openFragment(newFragment: Fragment, view: View? = null) {

      parentFragmentManager.commit {
        if (view != null) {
          addSharedElement(view, view.transitionName)
          newFragment.run {
            if (arguments == null) arguments = Bundle()
            arguments!!.putString("transitionName", view.transitionName)
          }
        }
        setReorderingAllowed(true)
        val oldFragment = this@openFragment
        add(oldFragment.id, newFragment)
        hide(oldFragment)
        addToBackStack(null)
      }
//      val uiViewModel by activityViewModels<MainViewModel>()
//      uiViewModel.isMainFragment.value = newFragment is MainFragment
    }

    fun FragmentActivity.openFragment(newFragment: Fragment, view: View? = null) {
      val oldFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
      oldFragment.openFragment(newFragment, view)
    }


  }
}
