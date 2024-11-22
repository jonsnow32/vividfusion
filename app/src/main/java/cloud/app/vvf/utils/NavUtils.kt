package cloud.app.vvf.utils

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import cloud.app.vvf.R

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

