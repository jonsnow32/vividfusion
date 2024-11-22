package cloud.app.vvf.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import com.google.android.material.R
import com.google.android.material.motion.MotionUtils

class ShimmerLayoutSelf @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

  companion object {
    private const val DEFAULT_ANIMATION_DURATION = 350L
  }

  private val shimmerAnimation: AlphaAnimation by lazy {
    AlphaAnimation(1.0f, 0.25f).apply {
      duration = resolvedAnimationDuration * 2
      fillAfter = true
      repeatMode = AlphaAnimation.REVERSE
      repeatCount = Animation.INFINITE
    }
  }

  // Lazily resolving theme duration to avoid unnecessary calls
  private val resolvedAnimationDuration: Long by lazy {
    MotionUtils.resolveThemeDuration(
      context,
      R.attr.motionDurationMedium1,
      DEFAULT_ANIMATION_DURATION.toInt()
    ).toLong() ?: DEFAULT_ANIMATION_DURATION
  }

  override fun onVisibilityChanged(changedView: View, visibility: Int) {
    super.onVisibilityChanged(changedView, visibility)
    if (visibility == View.VISIBLE) {
      startAnimation(shimmerAnimation)
    } else {
      clearAnimation()
    }
  }
}
