package cloud.app.vvf.utils.tv

import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.utils.toPx
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigationrail.NavigationRailView
import com.google.common.collect.Comparators
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

fun View.isLtr() = this.layoutDirection == View.LAYOUT_DIRECTION_LTR
fun View.isRtl() = this.layoutDirection == View.LAYOUT_DIRECTION_RTL

enum class FocusDirection {
  Start,
  End,
  Up,
  Down,
}


class FocusUtils {
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

}


val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
val screenWidth: Int
  get() {
    return max(displayMetrics.widthPixels, displayMetrics.heightPixels)
  }
val screenHeight: Int
  get() {
    return min(displayMetrics.widthPixels, displayMetrics.heightPixels)
  }

object TvFocus {
  data class FocusTarget(
    val width: Int,
    val height: Int,
    val x: Float,
    val y: Float,
  ) {
    companion object {
      fun lerp(a: FocusTarget, b: FocusTarget, lerp: Float): FocusTarget {
        val ilerp = 1 - lerp
        return FocusTarget(
          width = (a.width * ilerp + b.width * lerp).toInt(),
          height = (a.height * ilerp + b.height * lerp).toInt(),
          x = a.x * ilerp + b.x * lerp,
          y = a.y * ilerp + b.y * lerp
        )
      }
    }
  }

  var last: FocusTarget = FocusTarget(0, 0, 0.0f, 0.0f)
  var current: FocusTarget = FocusTarget(0, 0, 0.0f, 0.0f)

  var focusOutline: WeakReference<View> = WeakReference(null)
  var lastFocus: WeakReference<View> = WeakReference(null)
  private val layoutListener: View.OnLayoutChangeListener =
    View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      // shitty fix for layouts
      lastFocus.get()?.apply {
        updateFocusView(
          this, same = true
        )
        postDelayed({
          updateFocusView(
            lastFocus.get(), same = false
          )
        }, 300)
      }
    }
  private val attachListener: View.OnAttachStateChangeListener =
    object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {
        updateFocusView(v)
      }

      override fun onViewDetachedFromWindow(v: View) {
        // removes the focus view but not the listener as updateFocusView(null) will remove the listener
        focusOutline.get()?.isVisible = false
      }
    }
  /*private val scrollListener = object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          super.onScrolled(recyclerView, dx, dy)
          current = current.copy(x = current.x + dx, y = current.y + dy)
          setTargetPosition(current)
      }
  }*/

  private fun setTargetPosition(target: FocusTarget) {
    focusOutline.get()?.apply {
      layoutParams = layoutParams?.apply {
        width = target.width
        height = target.height
      }

      translationX = target.x
      translationY = target.y
      bringToFront()
    }
  }

  private var animator: ValueAnimator? = null

  /** if this is enabled it will keep the focus unmoving
   *  during listview move */
  private const val NO_MOVE_LIST: Boolean = false

  /** If this is enabled then it will try to move the
   * listview focus to the left instead of center */
  private const val LEFTMOST_MOVE_LIST: Boolean = true

  private val reflectedScroll by lazy {
    try {
      RecyclerView::class.java.declaredMethods.firstOrNull {
        it.name == "scrollStep"
      }?.also { it.isAccessible = true }
    } catch (t: Throwable) {
      null
    }
  }

  @MainThread
  fun updateFocusView(newFocus: View?, same: Boolean = false) {
    val focusOutline = focusOutline.get() ?: return
    val lastView = lastFocus.get()
    val exactlyTheSame = lastView == newFocus && newFocus != null
    if (!exactlyTheSame) {
      lastView?.removeOnLayoutChangeListener(layoutListener)
      lastView?.removeOnAttachStateChangeListener(attachListener)
      (lastView?.parent as? RecyclerView)?.apply {
        removeOnLayoutChangeListener(layoutListener)
        //removeOnScrollListener(scrollListener)
      }
    }

    val wasGone = focusOutline.isGone

    val visible =
      newFocus != null && newFocus.measuredHeight > 0 && newFocus.measuredWidth > 0 && newFocus.isShown && newFocus.tag != "tv_no_focus_tag"
    focusOutline.isVisible = visible

    if (newFocus != null) {
      lastFocus = WeakReference(newFocus)
      val parent = newFocus.parent
      var targetDx = 0
      if (parent is RecyclerView) {
        val layoutManager = parent.layoutManager
        if (layoutManager is LinearListLayout && layoutManager.orientation == LinearLayoutManager.HORIZONTAL) {
          val dx =
            LinearSnapHelper().calculateDistanceToFinalSnap(layoutManager, newFocus)
              ?.get(0)

          if (dx != null) {
            val rdx = if (LEFTMOST_MOVE_LIST) {
              // this makes the item the leftmost in ltr, instead of center
              val diff =
                ((layoutManager.width - layoutManager.paddingStart - newFocus.measuredWidth) / 2) - newFocus.marginStart
              dx + if (parent.isRtl()) {
                -diff
              } else {
                diff
              }
            } else {
              if (dx > 0) dx else 0
            }

            if (!NO_MOVE_LIST) {
              parent.smoothScrollBy(rdx, 0)
            } else {
              val smoothScroll = reflectedScroll
              if (smoothScroll == null) {
                parent.smoothScrollBy(rdx, 0)
              } else {
                try {
                  // this is very fucked but because it is a protected method to
                  // be able to compute the scroll I use reflection, scroll, then
                  // scroll back, then smooth scroll and set the no move
                  val out = IntArray(2)
                  smoothScroll.invoke(parent, rdx, 0, out)
                  val scrolledX = out[0]
                  if (abs(scrolledX) <= 0) { // newFocus.measuredWidth*2
                    smoothScroll.invoke(parent, -rdx, 0, out)
                    parent.smoothScrollBy(scrolledX, 0)
                    if (NO_MOVE_LIST) targetDx = scrolledX
                  }
                } catch (t: Throwable) {
                  parent.smoothScrollBy(rdx, 0)
                }
              }
            }
          }
        }
      }

      val out = IntArray(2)
      newFocus.getLocationInWindow(out)
      val (screenX, screenY) = out
      var (x, y) = screenX.toFloat() to screenY.toFloat()
      val (currentX, currentY) = focusOutline.translationX to focusOutline.translationY

      if (!newFocus.isLtr()) {
        x = x - focusOutline.rootView.width + newFocus.measuredWidth
      }
      x -= targetDx

      // out of bounds = 0,0
      if (screenX == 0 && screenY == 0) {
        focusOutline.isVisible = false
      }
      if (!exactlyTheSame) {
        (newFocus.parent as? RecyclerView)?.apply {
          addOnLayoutChangeListener(layoutListener)
          //addOnScrollListener(scrollListener)
        }
        newFocus.addOnLayoutChangeListener(layoutListener)
        newFocus.addOnAttachStateChangeListener(attachListener)
      }
      val start = FocusTarget(
        x = currentX,
        y = currentY,
        width = focusOutline.measuredWidth,
        height = focusOutline.measuredHeight
      )
      val end = FocusTarget(
        x = x,
        y = y,
        width = newFocus.measuredWidth,
        height = newFocus.measuredHeight
      )

      // if they are the same within then snap, aka scrolling
      val deltaMinX = Comparators.min(end.width / 2, 60.toPx)
      val deltaMinY = Comparators.min(end.height / 2, 60.toPx)
      if (start.width == end.width && start.height == end.height && (start.x - end.x).absoluteValue < deltaMinX && (start.y - end.y).absoluteValue < deltaMinY) {
        animator?.cancel()
        last = start
        current = end
        setTargetPosition(end)
        return
      }

      // if running then "reuse"
      if (animator?.isRunning == true) {
        current = end
        return
      } else {
        animator?.cancel()
      }


      last = start
      current = end

      // if previously gone, then tp
      if (wasGone) {
        setTargetPosition(current)
        return
      }

      // animate between a and b
      animator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
        startDelay = 0
        duration = 200
        addUpdateListener { animation ->
          val animatedValue = animation.animatedValue as Float
          val target = FocusTarget.lerp(last, current, minOf(animatedValue, 1.0f))
          setTargetPosition(target)
        }
        start()
      }

      // post check
      if (!same) {
        newFocus.postDelayed({
          updateFocusView(lastFocus.get(), same = true)
        }, 200)
      }

      /*

      the following is working, but somewhat bad code code

      if (!wasGone) {
          (focusOutline.parent as? ViewGroup)?.let {
              TransitionManager.endTransitions(it)
              TransitionManager.beginDelayedTransition(
                  it,
                  TransitionSet().addTransition(ChangeBounds())
                      .addTransition(ChangeTransform())
                      .setDuration(100)
              )
          }
      }

      focusOutline.layoutParams = focusOutline.layoutParams?.apply {
          width = newFocus.measuredWidth
          height = newFocus.measuredHeight
      }
      focusOutline.translationX = x.toFloat()
      focusOutline.translationY = y.toFloat()*/
    }
  }
}
