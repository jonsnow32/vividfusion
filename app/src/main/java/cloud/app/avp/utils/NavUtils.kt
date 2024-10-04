package cloud.app.avp.utils

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import cloud.app.avp.R
import cloud.app.avp.ui.setting.SettingsFragment
import com.google.android.material.appbar.MaterialToolbar

fun Fragment.navigate(dest: Fragment, transitionView: View? = null) {
  parentFragmentManager.commit {
    if (transitionView != null) {
      addSharedElement(transitionView, transitionView.transitionName)
      dest.run {
        if (arguments == null)
          arguments = Bundle()
        arguments!!.putString("transitionName", transitionView.transitionName)
      }
    }
    setCustomAnimations(0,0)
    setReorderingAllowed(true)
    val oldFragment = this@navigate
    add(oldFragment.id, dest)
    hide(oldFragment)
    addToBackStack(null)
  }
}

fun FragmentActivity.navigate(newFragment: Fragment, view: View? = null) {
  val oldFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
  oldFragment.navigate(newFragment, view)
}

fun MaterialToolbar.setUpMenu(fragment: Fragment) {
  return setOnMenuItemClickListener {
    when (it.itemId) {
      R.id.menu_settings -> {
        fragment.navigate(SettingsFragment())
        true
      }

      else -> false
    }
  }
}
