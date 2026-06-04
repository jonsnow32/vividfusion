package cloud.app.vvf.ui.main.files

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.databinding.ItemAudioFileBinding
import cloud.app.vvf.databinding.ItemVideoBinding
import com.bumptech.glide.Glide

class FilesAdapter(
  private val onVideoClick: (AVPMediaItem.VideoItem) -> Unit,
  private val onAudioClick: (AVPMediaItem.TrackItem) -> Unit,
) : PagingDataAdapter<AVPMediaItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

  companion object {
    const val VIEW_TYPE_VIDEO = 0
    const val VIEW_TYPE_AUDIO = 1

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AVPMediaItem>() {
      override fun areItemsTheSame(oldItem: AVPMediaItem, newItem: AVPMediaItem) = when {
        oldItem is AVPMediaItem.VideoItem && newItem is AVPMediaItem.VideoItem ->
          oldItem.video.uri == newItem.video.uri
        oldItem is AVPMediaItem.TrackItem && newItem is AVPMediaItem.TrackItem ->
          oldItem.track.uri == newItem.track.uri
        else -> false
      }

      override fun areContentsTheSame(oldItem: AVPMediaItem, newItem: AVPMediaItem) =
        oldItem == newItem
    }
  }

  override fun getItemViewType(position: Int) = when (peek(position)) {
    is AVPMediaItem.TrackItem -> VIEW_TYPE_AUDIO
    else -> VIEW_TYPE_VIDEO
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      VIEW_TYPE_AUDIO -> AudioViewHolder(
        ItemAudioFileBinding.inflate(inflater, parent, false)
      )
      else -> VideoViewHolder(
        ItemVideoBinding.inflate(inflater, parent, false)
      )
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val item = getItem(position) ?: return
    when {
      holder is VideoViewHolder && item is AVPMediaItem.VideoItem -> holder.bind(item)
      holder is AudioViewHolder && item is AVPMediaItem.TrackItem -> holder.bind(item)
    }
  }

  inner class VideoViewHolder(val binding: ItemVideoBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AVPMediaItem.VideoItem) {
      val video = item.video
      binding.textTitle.text = video.title
      binding.textDuration.text = FilesPagingSource.formatDuration(video.duration)
      Glide.with(binding.imageCover)
        .load(video.thumbnailUri?.let { Uri.parse(it) })
        .centerCrop()
        .into(binding.imageCover)
      binding.root.setOnClickListener { onVideoClick(item) }
    }
  }

  inner class AudioViewHolder(private val binding: ItemAudioFileBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AVPMediaItem.TrackItem) {
      val track = item.track
      binding.audioTitle.text = track.title
      binding.audioArtist.text = track.artists.firstOrNull()?.name
        ?.takeIf { it.isNotEmpty() } ?: track.album ?: ""
      binding.audioDuration.text = FilesPagingSource.formatDuration(track.duration)
      Glide.with(binding.albumArt)
        .load(track.cover)
        .placeholder(R.drawable.ic_music)
        .error(R.drawable.ic_music)
        .centerCrop()
        .into(binding.albumArt)
      binding.root.setOnClickListener { onAudioClick(item) }
    }
  }
}
