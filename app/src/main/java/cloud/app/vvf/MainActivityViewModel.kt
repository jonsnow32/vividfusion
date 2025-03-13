package cloud.app.vvf

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.datastore.app.helper.BookmarkItem
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.toPx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
  private val dataFlow: MutableStateFlow<AppDataStore>,

) : ViewModel() {


  val navInsets = MutableStateFlow(Insets())
  val systemInsets = MutableStateFlow(Insets())
  val bannerAdInsets = MutableStateFlow(Insets())

  val contentInsets = systemInsets.combine(navInsets) { system, nav ->
    system.add(nav)
  }.combine(bannerAdInsets) { insets, ad ->
    insets.add(ad)
  }

  fun setNavInsets(insets: Insets) {
    navInsets.value = insets
  }

  fun setBannerAdInsets(insets: Insets) {
    bannerAdInsets.value = insets
  }

  fun setSystemInsets(context: Context, insets: WindowInsetsCompat) {
    val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val inset = system.run {
      if (context.isRTL()) Insets(top, bottom, right, left)
      else Insets(top, bottom, left, right)
    }
    systemInsets.value = inset
  }


  companion object {
    fun Context.isRTL() =
      resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    fun Context.isLandscape() =
      resources.configuration.orientation == ORIENTATION_LANDSCAPE

    fun Context.isNightMode() =
      resources.configuration.uiMode and UI_MODE_NIGHT_MASK != UI_MODE_NIGHT_NO

    fun Fragment.applyInsets(block: MainActivityViewModel.(Insets) -> Unit) {
      val mainActivityViewModel by activityViewModels<MainActivityViewModel>()
      observe(mainActivityViewModel.contentInsets) { mainActivityViewModel.block(it) }
    }



    fun Fragment.applyInsetsMain(
      appBar: View,
      child: View,
      block: MainActivityViewModel.(Insets) -> Unit = {}
    ) {
      val mainActivityViewModel by activityViewModels<MainActivityViewModel>()
      observe(mainActivityViewModel.contentInsets) { insets ->
        child.applyContentInsets(insets)
        appBar.updatePaddingRelative(
          top = insets.top,
          start = insets.start,
          end = insets.end
        )
        mainActivityViewModel.block(insets)
      }
    }

    fun View.applyContentInsets(insets: Insets, paddingDp: Int = 8) {
      val verticalPadding = paddingDp.toPx
      updatePaddingRelative(
        top = verticalPadding,
        bottom = insets.bottom + verticalPadding,
        start = insets.start,
        end = insets.end
      )
    }

    fun View.applyInsets(it: Insets, paddingDp: Int = 0) {
      val padding = paddingDp.toPx
      updatePaddingRelative(
        top = it.top + padding,
        bottom = it.bottom + padding,
        start = it.start + padding,
        end = it.end + padding,
      )
    }

    fun View.applyFabInsets(it: Insets, system: Insets, paddingDp: Int = 0) {
      val padding = paddingDp.toPx
      updatePaddingRelative(
        bottom = it.bottom - system.bottom + padding,
        start = it.start + padding,
        end = it.end + padding,
      )
    }
  }


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


  fun addToBookmark(item: AVPMediaItem, type: String) {
    dataFlow.value.addToBookmark(item, type)
  }

  fun getBookmark(item: AVPMediaItem): BookmarkItem? {
    return dataFlow.value.findBookmark(item)
  }
}
