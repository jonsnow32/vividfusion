package cloud.app.vvf.ui.download

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.DownloadItem
import cloud.app.vvf.common.models.DownloadStatus
import cloud.app.vvf.common.models.getDownloadDisplayName
import cloud.app.vvf.common.models.getDownloadThumbnail
import cloud.app.vvf.databinding.ItemDownloadBinding
import com.bumptech.glide.Glide
import java.util.Locale

class DownloadsAdapter(
    private val onActionClick: (DownloadAction, DownloadItem) -> Unit
) : ListAdapter<DownloadItem, DownloadsAdapter.DownloadViewHolder>(DownloadDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DownloadViewHolder(binding, onActionClick)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DownloadViewHolder(
        private val binding: ItemDownloadBinding,
        private val onActionClick: (DownloadAction, DownloadItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(downloadItem: DownloadItem) {
            binding.apply {
                // Set title
                tvTitle.text = downloadItem.mediaItem.getDownloadDisplayName()

                // Set status text
                tvStatus.text = getStatusText(downloadItem)
                tvStatus.setTextColor(getStatusColor(downloadItem.status))

                // Load thumbnail
                Glide.with(binding.root.context)
                    .load(downloadItem.mediaItem.getDownloadThumbnail())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(ivThumbnail)

                // Setup progress bar
                setupProgressBar(downloadItem)

                // Setup download button
                setupDownloadButton(downloadItem)

                // Handle click actions
                btnDownloadAction.setOnClickListener {
                    val action = when (downloadItem.status) {
                        DownloadStatus.PENDING, DownloadStatus.DOWNLOADING -> DownloadAction.PAUSE
                        DownloadStatus.PAUSED -> DownloadAction.RESUME
                        DownloadStatus.FAILED -> DownloadAction.RETRY
                        DownloadStatus.CANCELLED -> DownloadAction.RETRY
                        DownloadStatus.COMPLETED -> DownloadAction.PLAY
                    }
                    onActionClick(action, downloadItem)
                }

                // Long click for remove option
                root.setOnLongClickListener {
                    onActionClick(DownloadAction.REMOVE, downloadItem)
                    true
                }
            }
        }

        private fun setupProgressBar(downloadItem: DownloadItem) {
            binding.apply {
                when (downloadItem.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED -> {
                        progressDownload.visibility = View.VISIBLE
                        progressDownload.progress = downloadItem.getProgressPercentage()

                        // Show circular progress
                        viewProgressBackground.visibility = View.VISIBLE
                        progressCircle.visibility = View.VISIBLE
                        progressCircle.progress = downloadItem.getProgressPercentage()
                    }
                    else -> {
                        progressDownload.visibility = View.GONE
                        viewProgressBackground.visibility = View.GONE
                        progressCircle.visibility = View.GONE
                    }
                }
            }
        }

        private fun setupDownloadButton(downloadItem: DownloadItem) {
            binding.apply {
                val iconRes = when (downloadItem.status) {
                    DownloadStatus.PENDING -> R.drawable.ic_pause_24
                    DownloadStatus.DOWNLOADING -> R.drawable.ic_pause_24
                    DownloadStatus.PAUSED -> R.drawable.ic_play_arrow_24
                    DownloadStatus.COMPLETED -> R.drawable.ic_play_arrow_24
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> R.drawable.ic_download_24
                }

                btnDownloadAction.setImageResource(iconRes)
                btnDownloadAction.contentDescription = getContentDescription(downloadItem.status)
            }
        }

        private fun getStatusText(downloadItem: DownloadItem): String {
            return when (downloadItem.status) {
                DownloadStatus.PENDING -> "Pending..."
                DownloadStatus.DOWNLOADING -> "${downloadItem.getProgressPercentage()}% • ${formatFileSize(downloadItem.downloadedBytes)} / ${formatFileSize(downloadItem.fileSize)}"
                DownloadStatus.PAUSED -> "Paused • ${downloadItem.getProgressPercentage()}%"
                DownloadStatus.COMPLETED -> "Completed • ${formatFileSize(downloadItem.fileSize)}"
                DownloadStatus.FAILED -> "Failed"
                DownloadStatus.CANCELLED -> "Cancelled"
            }
        }

        private fun getStatusColor(status: DownloadStatus): Int {
            val context = binding.root.context
            return when (status) {
                DownloadStatus.COMPLETED -> context.getColor(R.color.download_status_completed)
                DownloadStatus.DOWNLOADING -> context.getColor(R.color.download_status_downloading)
                DownloadStatus.FAILED -> context.getColor(R.color.download_status_failed)
                DownloadStatus.CANCELLED -> context.getColor(R.color.download_status_cancelled)
                else -> context.getColor(R.color.download_status_default)
            }
        }

        private fun getContentDescription(status: DownloadStatus): String {
            return when (status) {
                DownloadStatus.PENDING -> "Download pending"
                DownloadStatus.DOWNLOADING -> "Pause download"
                DownloadStatus.PAUSED -> "Resume download"
                DownloadStatus.COMPLETED -> "Play downloaded file"
                DownloadStatus.FAILED -> "Retry download"
                DownloadStatus.CANCELLED -> "Start download"
            }
        }

        private fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"

            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()

            return String.format(
                Locale.getDefault(),
                "%.1f %s",
                bytes / Math.pow(1024.0, digitGroups.toDouble()),
                units[digitGroups]
            )
        }
    }

    private class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem == newItem
        }
    }
}
