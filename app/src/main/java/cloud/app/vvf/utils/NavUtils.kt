package cloud.app.vvf.utils

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.extension.getExtension
import cloud.app.vvf.ui.detail.movie.MovieFragment
import cloud.app.vvf.ui.detail.show.ShowFragment
import cloud.app.vvf.ui.detail.show.season.SeasonFragment
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.ui.setting.ExtensionSettingFragment
import cloud.app.vvf.ui.setting.ManageExtensionsFragment
import cloud.app.vvf.viewmodels.SnackBarViewModel
import cloud.app.vvf.viewmodels.SnackBarViewModel.Companion.createSnack

fun Fragment.navigate(dest: Fragment, transitionView: View? = null, replace: Boolean = false) {
  parentFragmentManager.commit {
    if (transitionView != null) {
      addSharedElement(transitionView, transitionView.transitionName)
      dest.run {
        if (arguments == null)
          arguments = Bundle()
        arguments!!.putString("transitionName", transitionView.transitionName)
      }
    }
    setCustomAnimations(0, 0)
    setReorderingAllowed(true)
    val oldFragment = this@navigate
    if (replace) {
      replace(oldFragment.id, dest)
    } else {
      add(oldFragment.id, dest)
      hide(oldFragment)
    }
    addToBackStack(null)
  }
}


fun FragmentActivity.navigate(newFragment: Fragment, view: View? = null, replace: Boolean = false) {
  val oldFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
  oldFragment.navigate(newFragment, view, replace)
}

fun FragmentActivity.openItemFragmentFromUri(uri: Uri) {
  fun createSnack(id: Int) {
    val snackbar by viewModels<SnackBarViewModel>()
    val message = getString(id)
    snackbar.create(SnackBarViewModel.Message(message))
  }
  when (uri.host) {
    "extensions" -> {
      val path = uri.pathSegments
      val id = path[0]
      if (id.isNullOrEmpty())
        this.navigate(ManageExtensionsFragment())
      else {
        //this.navigate(ExtensionSettingFragment.newInstance(id, ""), null, true)
      }
    }

    else -> {
      createSnack(R.string.something_went_wrong)
    }
  }
}

fun Fragment.navigate(
  item: AVPMediaItem,
  extensionId: String? = null,
  transitionView: View? = null
) {
  val bundle = Bundle()
  bundle.putString("extensionId", extensionId)
  bundle.putSerialized("mediaItem", item)

  when (item) {
    is AVPMediaItem.MovieItem -> {
      val movieFragment = MovieFragment()
      movieFragment.arguments = bundle;
      navigate(movieFragment, transitionView)
    }

    is AVPMediaItem.ShowItem -> {
      val showFragment = ShowFragment()
      showFragment.arguments = bundle;
      navigate(showFragment, transitionView)
    }

    is AVPMediaItem.SeasonItem -> {
      val seasonFragment = SeasonFragment();
      seasonFragment.arguments = bundle;
      navigate(
        seasonFragment,
        transitionView
      )
    }

    else -> createSnack(getString(R.string.not_implemented))
  }
}

