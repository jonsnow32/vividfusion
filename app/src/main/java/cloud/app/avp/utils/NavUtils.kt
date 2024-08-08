package cloud.app.avp.utils

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import cloud.app.avp.R
import com.google.android.material.appbar.MaterialToolbar

fun Fragment.navigate(@IdRes dest: Int, view: View? = null, bundle: Bundle? = null) {
  val extras = view?.let {
    FragmentNavigatorExtras(it to it.transitionName)
  }
  val args = bundle ?: Bundle().apply {
    view?.let {
      putString("transitionName", it.transitionName)
    }
  }

  findNavController().navigate(dest, args, null, extras)
}

fun MaterialToolbar.setUpMenu(fragment: Fragment) {
  return setOnMenuItemClickListener {
    when (it.itemId) {
      R.id.menu_settings -> {
        fragment.navigate(R.id.settingsFragment)
        true
      }
      else -> false
    }
  }
}
