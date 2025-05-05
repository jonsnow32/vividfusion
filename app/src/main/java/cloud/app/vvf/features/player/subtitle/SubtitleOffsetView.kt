package cloud.app.vvf.features.player.subtitle

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.ViewSubtitleOffsetBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
class SubtitleOffsetView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

  private val binding: ViewSubtitleOffsetBinding =
    ViewSubtitleOffsetBinding.inflate(LayoutInflater.from(context), this, true)
  private lateinit var adapter: SubtitleCueAdapter
  private var currentOffsetMs: Long = 0
  private var currentTimeMs: Long = 0
  private var currentSubtitlePos = -1
  private var subtitleCues: List<SubtitleCue> = emptyList()
  private var shouldAutoScroll = true

  private var isInit = false

  val layoutManager by lazy {
    binding.rvSubtitlesOffset.layoutManager as LinearLayoutManager
  }

  var callback: ((Long) -> Unit)? = null

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    binding.rvSubtitlesOffset.layoutManager = LinearLayoutManager(context)
    binding.subtitleOffsetReset.setOnClickListener {
      currentOffsetMs = 0
      binding.subtitleOffsetInput.setText("0")
      callback?.invoke(currentOffsetMs)
    }
    val lifecycleOwner = findViewTreeLifecycleOwner()
    var job: Job? = null
    binding.rvSubtitlesOffset.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy > 0) return
        shouldAutoScroll = false
        job?.cancel()
        job = lifecycleOwner?.lifecycleScope?.launch {
          delay(3500)
          shouldAutoScroll = true
        }
      }
    })

    binding.subtitleOffsetClose.setOnClickListener {
      binding.root.isGone = true
      callback?.invoke(currentOffsetMs)
      job?.cancel()
    }
    adapter = SubtitleCueAdapter(
      currentTimeMs = currentTimeMs,
      clickCallback = { cue ->
        if (cue.startTimeMs != Long.MAX_VALUE) {
          val detaTime = System.currentTimeMillis() - lastUpdatedMs;
          currentOffsetMs = (currentTimeMs + detaTime) - cue.startTimeMs
          binding.subtitleOffsetInput.setText(currentOffsetMs.toString())
          updateAdapterCues(currentTimeMs)
          callback?.invoke(currentOffsetMs)
        }
      }
    )

    binding.rvSubtitlesOffset.adapter = adapter
  }

  fun initialize(
    cues: List<SubtitleCue>,
    currentTimeMs: Long,
    currentOffset: Long,
    callback: (Long) -> Unit
  ) {
    this.subtitleCues = cues
    this.currentTimeMs = currentTimeMs
    this.currentOffsetMs = currentOffset
    shouldAutoScroll = true
    updateVisibility()
    binding.root.isGone = false
    adapter.submitList(cues)
    this.callback = callback
    isInit = true;
    updateAdapterCues(currentTimeMs)
  }

  var lastUpdatedMs = 0L;
  fun updateAdapterCues(current: Long) {
    if (!isInit) return

    currentTimeMs = current;
    lastUpdatedMs = System.currentTimeMillis()
    val updatedCues = subtitleCues.map { cue ->
      cue.copy(startTimeMs = cue.startTimeMs + currentOffsetMs)
    }

    val currentTime = updatedCues.getOrNull(currentSubtitlePos)?.endTimeMs ?: -1
    if (currentTime < current || current <= 0) {
      val currentIndex = updatedCues.indexOfLast { subtitle ->
        subtitle.startTimeMs <= current
      }
      adapter.updateCurrent(currentIndex)
      if (!shouldAutoScroll) return
//      slideDown()
      if (currentIndex < 0) return
      val smoothScroller = CenterSmoothScroller(context)
      smoothScroller.targetPosition = currentIndex
      layoutManager.startSmoothScroll(smoothScroller)
    }
  }

  class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
    override fun calculateDtToFit(
      viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int
    ): Int {
      val midPoint = boxEnd / 2
      val targetMidPoint = ((viewEnd - viewStart) / 2) + viewStart
      return midPoint - targetMidPoint
    }

    override fun getVerticalSnapPreference() = SNAP_TO_START
    override fun calculateTimeForDeceleration(dx: Int) = 650
  }

  private fun updateVisibility() {
    binding.noSubtitlesLoadedNotice.visibility = if (subtitleCues.isNotEmpty()) GONE else VISIBLE
    binding.rvSubtitlesOffset.visibility = if (subtitleCues.isNotEmpty()) VISIBLE else GONE
  }

}
