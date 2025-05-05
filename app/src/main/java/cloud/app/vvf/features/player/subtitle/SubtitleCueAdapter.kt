package cloud.app.vvf.features.player.subtitle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.databinding.SubtitleOffsetItemBinding
import cloud.app.vvf.utils.applyTranslationYAnimation

data class SubtitleCue(val startTimeMs: Long, val durationMs: Long, val text: List<String>) {
  val endTimeMs = startTimeMs + durationMs
  override fun toString(): String {
    return text.joinToString("\n")
  }
}

class SubtitleCueAdapter(
  private var currentTimeMs: Long,
  private val clickCallback: (SubtitleCue) -> Unit
) : ListAdapter<SubtitleCue, SubtitleCueAdapter.ViewHolder>(DiffCallback) {

  object DiffCallback : DiffUtil.ItemCallback<SubtitleCue>() {
    override fun areItemsTheSame(oldItem: SubtitleCue, newItem: SubtitleCue) =
      oldItem.text == newItem.text

    override fun areContentsTheSame(oldItem: SubtitleCue, newItem: SubtitleCue) =
      oldItem == newItem
  }

  inner class ViewHolder(val binding: SubtitleOffsetItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    init {
      binding.root.setOnClickListener {
        val subtitle = getItem(bindingAdapterPosition) ?: return@setOnClickListener
        clickCallback(subtitle)
      }
    }
  }

  private fun ViewHolder.updateColors() {
    binding.subtitleText.run {
      val colors = ContextCompat.getColor(context, R.color.white)
      val alphaStrippedColor = colors or -0x1000000
      setTextColor(alphaStrippedColor)
    }
  }

  private fun getItemOrNull(position: Int) = if (position >= 0) getItem(position) else null

  private var currentPos = -1
  private fun ViewHolder.updateCurrent() {
    val currentTime = getItemOrNull(currentPos)?.startTimeMs ?: 0
    val time = getItemOrNull(bindingAdapterPosition)?.startTimeMs ?: 0
    binding.root.alpha = if (currentTime >= time) 1f else 0.5f
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = SubtitleOffsetItemBinding.inflate(inflater, parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val subtitle = getItem(position) ?: return
    holder.binding.subtitleText.text = "${subtitle.startTimeMs} - ${subtitle.toString().trim().trim('\n').ifEmpty { "♪" }}"
    holder.updateColors()
    holder.updateCurrent()
    holder.itemView.applyTranslationYAnimation(scrollAmount)
  }

  override fun onViewAttachedToWindow(holder: ViewHolder) {
    holder.updateColors()
  }

  private var scrollAmount: Int = 0
  private val scrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      scrollAmount = dy
    }
  }

  var recyclerView: RecyclerView? = null
  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    this.recyclerView = recyclerView
    recyclerView.addOnScrollListener(scrollListener)
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    recyclerView.removeOnScrollListener(scrollListener)
    this.recyclerView = null
  }

  private fun onEachViewHolder(block: ViewHolder.() -> Unit) {
    recyclerView?.let { rv ->
      for (i in 0 until rv.childCount) {
        val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? ViewHolder ?: continue
        holder.block()
      }
    }
  }

  fun updateColors() {
    onEachViewHolder { updateColors() }
  }

  fun updateCurrent(currentPos: Int) {
    this.currentPos = currentPos
    onEachViewHolder { updateCurrent() }
  }
}
