package cloud.app.avp.utils.tv

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import cloud.app.avp.R
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigationrail.NavigationRailView

class NextFocuser {
  fun continueGetNextFocus(
    root: Any?,
    view: View,
    direction: FocusDirection,
    nextId: Int,
    depth: Int = 0
  ): View? {
    if (nextId == View.NO_ID) return null

    // do an initial search for the view, in case the localLook is too deep we can use this as
    // an early break and backup view
    var next =
      when (root) {
        is Activity -> root.findViewById(nextId)
        is View -> root.rootView.findViewById<View?>(nextId)
        else -> null
      } ?: return null

    next = localLook(view, nextId) ?: next
    val shown = next.hasContent()

    // if cant focus but visible then break and let android decide
    // the exception if is the view is a parent and has children that wants focus
    val hasChildrenThatWantsFocus = (next as? ViewGroup)?.let { parent ->
      parent.descendantFocusability == ViewGroup.FOCUS_AFTER_DESCENDANTS && parent.childCount > 0
    } ?: false
    if (!next.isFocusable && shown && !hasChildrenThatWantsFocus) return null

    // if not shown then continue because we will "skip" over views to get to a replacement
    if (!shown) {
      // we don't want a while true loop, so we let android decide if we find a recursive view
      if (next == view) return null
      return getNextFocus(root, next, direction, depth + 1)
    }

    (when (next) {
      is ChipGroup -> {
        next.children.firstOrNull { it.isFocusable && it.isShown }
      }

      is NavigationRailView -> {
        next.findViewById(next.selectedItemId) ?: next.findViewById(R.id.mainFragment)
      }

      else -> null
    })?.let {
      return it
    }

    // nothing wrong with the view found, return it
    return next
  }

  private fun localLook(from: View, id: Int): View? {
    if (id == View.NO_ID) return null
    var currentLook: View = from
    // limit to 15 look depth
    for (i in 0..15) {
      currentLook.findViewById<View?>(id)?.let { return it }
      currentLook = (currentLook.parent as? View) ?: break
    }
    return null
  }

  /** recursively looks for a next focus up to a depth of 10,
   * this is used to override the normal shit focus system
   * because this application has a lot of invisible views that messes with some tv devices*/
  fun getNextFocus(
    root: Any?,
    view: View?,
    direction: FocusDirection,
    depth: Int = 0
  ): View? {
    // if input is invalid let android decide + depth test to not crash if loop is found
    if (view == null || depth >= 10 || root == null) {
      return null
    }

    var nextId = when (direction) {
      FocusDirection.Start -> {
        if (view.isRtl())
          view.nextFocusRightId
        else
          view.nextFocusLeftId
      }

      FocusDirection.Up -> {
        view.nextFocusUpId
      }

      FocusDirection.End -> {
        if (view.isRtl())
          view.nextFocusLeftId
        else
          view.nextFocusRightId
      }

      FocusDirection.Down -> {
        view.nextFocusDownId
      }
    }

    if (nextId == View.NO_ID) {
      // if not specified then use forward id
      nextId = view.nextFocusForwardId
      // if view is still not found to next focus then return and let android decide
      if (nextId == View.NO_ID)
        return null
    }
    return continueGetNextFocus(root, view, direction, nextId, depth)
  }
  private fun View.hasContent(): Boolean {
    return isShown && when (this) {
      //is RecyclerView -> this.childCount > 0
      is ViewGroup -> this.childCount > 0
      else -> true
    }
  }
  fun View.isLtr() = this.layoutDirection == View.LAYOUT_DIRECTION_LTR
  fun View.isRtl() = this.layoutDirection == View.LAYOUT_DIRECTION_RTL

}
