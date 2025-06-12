package cloud.app.vvf.ui.stream

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.databinding.SkeletonItemStreamBinding

class StreamLoadStateAdapter(private val retry: () -> Unit) :
  RecyclerView.Adapter<StreamLoadStateAdapter.ViewHolder>() {

  var isLoading: Boolean = false
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = SkeletonItemStreamBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(isLoading)
  }

  override fun getItemCount() = if (isLoading) 1 else 0

  inner class ViewHolder(private val binding: SkeletonItemStreamBinding) :
    RecyclerView.ViewHolder(binding.root) {

    init {
      //binding.retryButton.setOnClickListener { retry.invoke() }
    }

    fun bind(isLoading: Boolean) {
      binding.apply {
        // Assuming SkeletonItemStreamBinding has a progress bar or loading indicator
        // Adjust visibility based on isLoading state
        root.isVisible = isLoading
      }
    }
  }
}
