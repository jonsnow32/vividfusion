package cloud.app.avp.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.marginEnd
import cloud.app.avp.R
import kotlin.math.max

class FlowLayout : ViewGroup {
  var itemSpacing: Int = 0

  constructor(context: Context?) : super(context)

  @SuppressLint("CustomViewStyleable")
  internal constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
    val t = c.obtainStyledAttributes(attrs, R.styleable.FlowLayout_Layout)
    itemSpacing = t.getDimensionPixelSize(R.styleable.FlowLayout_Layout_itemSpacing, 0)
    t.recycle()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val realWidth = MeasureSpec.getSize(widthMeasureSpec)
    var currentHeight = 0
    var currentWidth = 0
    var currentChildHookPointx = 0
    var currentChildHookPointy = 0
    val childCount = this.childCount

    for (i in 0 until childCount) {
      val child = getChildAt(i)

      // Skip measuring and laying out GONE children
      if (child.visibility == GONE) continue

      measureChild(child, widthMeasureSpec, heightMeasureSpec)
      val childWidth = child.measuredWidth
      val childHeight = child.measuredHeight

      // Check if the child can fit in the current row
      if (currentChildHookPointx + childWidth > realWidth) {
        // Move to the next line
        currentChildHookPointx = 0
        currentChildHookPointy += childHeight // Move the y-coordinate down
      }

      // Set the position for the current child
      val lp = child.layoutParams as LayoutParams
      lp.x = currentChildHookPointx
      lp.y = currentChildHookPointy

      // Update x position for the next child in the same row
      // Only add itemSpacing if the current child is visible
      currentChildHookPointx += childWidth + if (i != childCount - 1) itemSpacing else 0

      // Update the height to reflect the total height occupied so far
      currentHeight = max(currentHeight, currentChildHookPointy + childHeight)
      currentWidth = max(currentWidth, currentChildHookPointx)
    }

    // Set the measured dimensions
    setMeasuredDimension(
      resolveSize(currentWidth, widthMeasureSpec), resolveSize(currentHeight, heightMeasureSpec)
    )
  }

  override fun onLayout(b: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    // Call layout on children
    val childCount = this.childCount
    for (i in 0 until childCount) {
      val child = getChildAt(i)

      // Skip GONE children
      if (child.visibility == GONE) continue

      val lp = child.layoutParams as LayoutParams
      child.layout(lp.x, lp.y, lp.x + child.measuredWidth, lp.y + child.measuredHeight)
    }
  }

  override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
    return LayoutParams(context, attrs)
  }

  override fun generateDefaultLayoutParams(): LayoutParams {
    return LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
    return LayoutParams(p)
  }

  override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
    return p is LayoutParams
  }

  class LayoutParams : MarginLayoutParams {
    var x = 0
    var y = 0

    @SuppressLint("CustomViewStyleable")
    internal constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
      val t = c.obtainStyledAttributes(attrs, R.styleable.FlowLayout_Layout)
      t.recycle()
    }

    internal constructor(width: Int, height: Int) : super(width, height)

    constructor(source: MarginLayoutParams?) : super(source)
    internal constructor(source: ViewGroup.LayoutParams?) : super(source)
  }
}
