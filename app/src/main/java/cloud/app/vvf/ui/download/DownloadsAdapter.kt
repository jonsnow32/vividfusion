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

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            // Full update
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Partial update based on payload
            holder.bindPartial(getItem(position), payloads)
        }
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
                        DownloadStatus.PENDING -> DownloadAction.CANCEL
                        DownloadStatus.DOWNLOADING -> DownloadAction.PAUSE
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

        fun bindPartial(downloadItem: DownloadItem, payloads: MutableList<Any>) {
            binding.apply {
                // Update fields based on payload
                for (payload in payloads) {
                    if (payload is List<*>) {
                        for (changeType in payload) {
                            when (changeType) {
                                PAYLOAD_STATUS -> {
                                    // Update status text and color
                                    tvStatus.text = getStatusText(downloadItem)
                                    tvStatus.setTextColor(getStatusColor(downloadItem.status))

                                    // Update download button icon
                                    setupDownloadButton(downloadItem)
                                }
                                PAYLOAD_PROGRESS -> {
                                    // Update progress bar and status text
                                    setupProgressBar(downloadItem)
                                    tvStatus.text = getStatusText(downloadItem)
                                }
                            }
                        }
                    }
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
                    DownloadStatus.PENDING -> R.drawable.ic_close
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
                DownloadStatus.DOWNLOADING -> {
                    val progressText = "${downloadItem.getProgressPercentage()}% • ${formatFileSize(downloadItem.downloadedBytes)} / ${formatFileSize(downloadItem.fileSize)}"
                    val speedText = if (downloadItem.downloadSpeed > 0) " • ${downloadItem.getFormattedSpeed()}" else ""
                    val connectionsText = if (downloadItem.connections > 1) " • ${downloadItem.connections} connections" else ""
                    val etaText = if (downloadItem.downloadSpeed > 0) " • ETA: ${downloadItem.getEstimatedTimeRemaining()}" else ""
                    "$progressText$speedText$connectionsText$etaText"
                }
                DownloadStatus.PAUSED -> {
                    val progressText = "Paused • ${downloadItem.getProgressPercentage()}%"
                    val connectionsText = if (downloadItem.connections > 1) " • ${downloadItem.connections} connections" else ""
                    "$progressText$connectionsText"
                }
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
                DownloadStatus.PENDING -> "Cancel download" // Thay đổi từ "Download pending"
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
            // Compare all important fields that affect UI display
            return oldItem.status == newItem.status &&
                    oldItem.progress == newItem.progress &&
                    oldItem.fileName == newItem.fileName &&
                    oldItem.url == newItem.url &&
                    oldItem.localPath == newItem.localPath &&
                    oldItem.fileSize == newItem.fileSize
        }

        override fun getChangePayload(oldItem: DownloadItem, newItem: DownloadItem): Any? {
            val payload = mutableListOf<String>()

            // Only add status payload if status actually changed
            if (oldItem.status != newItem.status) {
                payload.add(PAYLOAD_STATUS)
            }

            // Add progress payload if progress changed
            if (oldItem.progress != newItem.progress) {
                payload.add(PAYLOAD_PROGRESS)
            }

            return if (payload.isEmpty()) null else payload
        }
    }

    companion object {
        private const val PAYLOAD_STATUS = "status"
        private const val PAYLOAD_PROGRESS = "progress"
    }
}
