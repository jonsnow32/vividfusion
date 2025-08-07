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
import cloud.app.vvf.databinding.ItemDownloadHttpBinding
import cloud.app.vvf.databinding.ItemDownloadHlsBinding
import cloud.app.vvf.databinding.ItemDownloadTorrentBinding
import com.bumptech.glide.Glide
import java.util.Locale

class DownloadsAdapter(
    private val onActionClick: (DownloadAction, DownloadItem) -> Unit
) : ListAdapter<DownloadItem, DownloadsAdapter.BaseDownloadViewHolder>(DownloadDiffCallback()) {

    companion object {
        const val VIEW_TYPE_HTTP = 1
        const val VIEW_TYPE_HLS = 2
        const val VIEW_TYPE_TORRENT = 3

        const val PAYLOAD_STATUS = "status"
        const val PAYLOAD_PROGRESS = "progress"
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DownloadItem.HttpDownload -> VIEW_TYPE_HTTP
            is DownloadItem.HlsDownload -> VIEW_TYPE_HLS
            is DownloadItem.TorrentDownload -> VIEW_TYPE_TORRENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseDownloadViewHolder {
        return when (viewType) {
            VIEW_TYPE_HTTP -> {
                val binding = ItemDownloadHttpBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HttpDownloadViewHolder(binding, onActionClick)
            }
            VIEW_TYPE_HLS -> {
                val binding = ItemDownloadHlsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HlsDownloadViewHolder(binding, onActionClick)
            }
            VIEW_TYPE_TORRENT -> {
                val binding = ItemDownloadTorrentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                TorrentDownloadViewHolder(binding, onActionClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseDownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: BaseDownloadViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindPartial(getItem(position), payloads)
        }
    }

    // Base ViewHolder cho common functionality
    abstract class BaseDownloadViewHolder(
        itemView: View,
        protected val onActionClick: (DownloadAction, DownloadItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        abstract fun bind(downloadItem: DownloadItem)
        abstract fun bindPartial(downloadItem: DownloadItem, payloads: MutableList<Any>)

        protected fun getStatusColor(status: DownloadStatus): Int {
            val context = itemView.context
            return when (status) {
                DownloadStatus.COMPLETED -> context.getColor(R.color.download_status_completed)
                DownloadStatus.DOWNLOADING -> context.getColor(R.color.download_status_downloading)
                DownloadStatus.FAILED -> context.getColor(R.color.download_status_failed)
                DownloadStatus.CANCELLED -> context.getColor(R.color.download_status_cancelled)
                else -> context.getColor(R.color.download_status_default)
            }
        }

        protected fun getDownloadActionIcon(status: DownloadStatus): Int {
            return when (status) {
                DownloadStatus.PENDING -> R.drawable.ic_close
                DownloadStatus.DOWNLOADING -> R.drawable.ic_pause_24
                DownloadStatus.PAUSED -> R.drawable.ic_play_arrow_24
                DownloadStatus.COMPLETED -> R.drawable.ic_play_arrow_24
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> R.drawable.ic_download_24
            }
        }

        protected fun getDownloadAction(status: DownloadStatus): DownloadAction {
            return when (status) {
                DownloadStatus.PENDING -> DownloadAction.CANCEL
                DownloadStatus.DOWNLOADING -> DownloadAction.PAUSE
                DownloadStatus.PAUSED -> DownloadAction.RESUME
                DownloadStatus.FAILED -> DownloadAction.RETRY
                DownloadStatus.CANCELLED -> DownloadAction.RETRY
                DownloadStatus.COMPLETED -> DownloadAction.PLAY
            }
        }

        protected fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
            return String.format(
                Locale.getDefault(),
                "%.1f %s",
                bytes / Math.pow(1024.0, digitGroups.toDouble()),
                units[digitGroups]
            )
        }
    }

    // HTTP Download ViewHolder
    class HttpDownloadViewHolder(
        private val binding: ItemDownloadHttpBinding,
        onActionClick: (DownloadAction, DownloadItem) -> Unit
    ) : BaseDownloadViewHolder(binding.root, onActionClick) {

        override fun bind(downloadItem: DownloadItem) {
            val httpDownload = downloadItem as DownloadItem.HttpDownload

            binding.apply {
                // Basic info
                tvTitle.text = downloadItem.mediaItem.getDownloadDisplayName()
                tvDownloadType.text = "HTTP"

                // HTTP-specific info
                tvConnections.text = "Connections: ${httpDownload.connections}"
                tvResumeSupport.text = if (httpDownload.resumeSupported) "Resume: Yes" else "Resume: No"
                tvResumeSupport.setTextColor(
                    if (httpDownload.resumeSupported)
                        root.context.getColor(R.color.download_status_completed)
                    else
                        root.context.getColor(R.color.download_status_cancelled)
                )

                // Status and progress
                updateProgress(httpDownload)

                // Setup download button widget
                setupDownloadButtonWidget(downloadItem)

                // Long click for remove
                root.setOnLongClickListener {
                    onActionClick(DownloadAction.REMOVE, downloadItem)
                    true
                }
            }
        }

        override fun bindPartial(downloadItem: DownloadItem, payloads: MutableList<Any>) {
            val httpDownload = downloadItem as DownloadItem.HttpDownload

            for (payload in payloads) {
                if (payload is List<*>) {
                    for (changeType in payload) {
                        when (changeType) {
                            PAYLOAD_STATUS -> {
                                binding.downloadButtonWidget.updateState(downloadItem.status, httpDownload.getProgressPercentage())
                                updateProgress(httpDownload)
                            }
                            PAYLOAD_PROGRESS -> {
                                binding.downloadButtonWidget.updateState(downloadItem.status, httpDownload.getProgressPercentage())
                                updateProgress(httpDownload)
                            }
                        }
                    }
                }
            }
        }

        private fun setupDownloadButtonWidget(downloadItem: DownloadItem) {
            val httpDownload = downloadItem as DownloadItem.HttpDownload

            binding.downloadButtonWidget.apply {
                // Update the widget state
                updateState(downloadItem.status, httpDownload.getProgressPercentage())

                // Set up click handlers based on download status
                onDownloadClick = {
                    onActionClick(DownloadAction.RETRY, downloadItem)
                }

                onPauseClick = {
                    onActionClick(DownloadAction.PAUSE, downloadItem)
                }

                onResumeClick = {
                    onActionClick(DownloadAction.RESUME, downloadItem)
                }

                onCancelClick = {
                    onActionClick(DownloadAction.CANCEL, downloadItem)
                }

                onPlayClick = {
                    onActionClick(DownloadAction.PLAY, downloadItem)
                }
            }
        }

        private fun updateProgress(httpDownload: DownloadItem.HttpDownload) {
            binding.apply {
                when (httpDownload.status) {
                    DownloadStatus.DOWNLOADING -> {
                        val progressText = "${httpDownload.getProgressPercentage()}%"
                        val sizeText = "${formatFileSize(httpDownload.downloadedBytes)} / ${formatFileSize(httpDownload.fileSize)}"
                        val speedText = if (httpDownload.downloadSpeed > 0) httpDownload.getFormattedSpeed() else "0 B/s"
                        val etaText = if (httpDownload.downloadSpeed > 0) "ETA: ${httpDownload.getEstimatedTimeRemaining()}" else ""

                        tvStatus.text = "$progressText • $sizeText"
                        tvSpeed.text = speedText
                        tvEta.text = etaText
                        tvStatus.setTextColor(getStatusColor(httpDownload.status))
                    }
                    DownloadStatus.PAUSED -> {
                        tvStatus.text = "Paused • ${httpDownload.getProgressPercentage()}%"
                        tvSpeed.text = ""
                        tvEta.text = ""
                        tvStatus.setTextColor(getStatusColor(httpDownload.status))
                    }
                    DownloadStatus.COMPLETED -> {
                        tvStatus.text = "Completed • ${formatFileSize(httpDownload.fileSize)}"
                        tvSpeed.text = ""
                        tvEta.text = ""
                        tvStatus.setTextColor(getStatusColor(httpDownload.status))
                    }
                    else -> {
                        tvStatus.text = httpDownload.status.name.lowercase().replaceFirstChar { it.uppercase() }
                        tvSpeed.text = ""
                        tvEta.text = ""
                        tvStatus.setTextColor(getStatusColor(httpDownload.status))
                    }
                }
            }
        }
    }

    // HLS Download ViewHolder
    class HlsDownloadViewHolder(
        private val binding: ItemDownloadHlsBinding,
        onActionClick: (DownloadAction, DownloadItem) -> Unit
    ) : BaseDownloadViewHolder(binding.root, onActionClick) {

        override fun bind(downloadItem: DownloadItem) {
            val hlsDownload = downloadItem as DownloadItem.HlsDownload

            binding.apply {
                // Basic info
                tvTitle.text = downloadItem.mediaItem.getDownloadDisplayName()
                tvDownloadType.text = "HLS"

                // HLS-specific info
                tvQuality.text = "Quality: ${hlsDownload.quality}"
                tvSegments.text = hlsDownload.getSegmentProgress()
                if (hlsDownload.encryption != null) {
                    tvEncryption.text = "Encrypted: ${hlsDownload.encryption}"
                    tvEncryption.visibility = View.VISIBLE
                } else {
                    tvEncryption.visibility = View.GONE
                }

                // Status and progress
                updateProgress(hlsDownload)

                // Setup download button widget
                setupDownloadButtonWidget(downloadItem)

                // Long click for remove
                root.setOnLongClickListener {
                    onActionClick(DownloadAction.REMOVE, downloadItem)
                    true
                }
            }
        }

        override fun bindPartial(downloadItem: DownloadItem, payloads: MutableList<Any>) {
            val hlsDownload = downloadItem as DownloadItem.HlsDownload

            for (payload in payloads) {
                if (payload is List<*>) {
                    for (changeType in payload) {
                        when (changeType) {
                            PAYLOAD_STATUS -> {
                                binding.downloadButtonWidget.updateState(downloadItem.status, hlsDownload.getProgressPercentage())
                                updateProgress(hlsDownload)
                            }
                            PAYLOAD_PROGRESS -> {
                                binding.downloadButtonWidget.updateState(downloadItem.status, hlsDownload.getProgressPercentage())
                                updateProgress(hlsDownload)
                                binding.tvSegments.text = hlsDownload.getSegmentProgress()
                            }
                        }
                    }
                }
            }
        }

        private fun setupDownloadButtonWidget(downloadItem: DownloadItem) {
            val hlsDownload = downloadItem as DownloadItem.HlsDownload

            binding.downloadButtonWidget.apply {
                // Update the widget state
                updateState(downloadItem.status, hlsDownload.getProgressPercentage())

                // Set up click handlers based on download status
                onDownloadClick = {
                    onActionClick(DownloadAction.RETRY, downloadItem)
                }

                onPauseClick = {
                    onActionClick(DownloadAction.PAUSE, downloadItem)
                }

                onResumeClick = {
                    onActionClick(DownloadAction.RESUME, downloadItem)
                }

                onCancelClick = {
                    onActionClick(DownloadAction.CANCEL, downloadItem)
                }

                onPlayClick = {
                    onActionClick(DownloadAction.PLAY, downloadItem)
                }
            }
        }

        private fun updateProgress(hlsDownload: DownloadItem.HlsDownload) {
            binding.apply {
                when (hlsDownload.status) {
                    DownloadStatus.DOWNLOADING -> {
                        val progressText = "${hlsDownload.getProgressPercentage()}%"
                        val speedText = if (hlsDownload.downloadSpeed > 0) hlsDownload.getFormattedSpeed() else "0 B/s"

                        tvStatus.text = "Downloading • $progressText"
                        tvSpeed.text = speedText
                        tvStatus.setTextColor(getStatusColor(hlsDownload.status))
                    }
                    DownloadStatus.PAUSED -> {
                        tvStatus.text = "Paused • ${hlsDownload.getProgressPercentage()}%"
                        tvSpeed.text = ""
                        tvStatus.setTextColor(getStatusColor(hlsDownload.status))
                    }
                    DownloadStatus.COMPLETED -> {
                        tvStatus.text = "Completed • ${formatFileSize(hlsDownload.fileSize)}"
                        tvSpeed.text = ""
                        tvStatus.setTextColor(getStatusColor(hlsDownload.status))
                    }
                    else -> {
                        tvStatus.text = hlsDownload.status.name.lowercase().replaceFirstChar { it.uppercase() }
                        tvSpeed.text = ""
                        tvStatus.setTextColor(getStatusColor(hlsDownload.status))
                    }
                }
            }
        }
    }

    // Torrent Download ViewHolder
    class TorrentDownloadViewHolder(
        private val binding: ItemDownloadTorrentBinding,
        onActionClick: (DownloadAction, DownloadItem) -> Unit
    ) : BaseDownloadViewHolder(binding.root, onActionClick) {

        // Cache for real-time torrent data from WorkManager
        private var realTimeTorrentData: Map<String, Any> = emptyMap()

        override fun bind(downloadItem: DownloadItem) {
            val torrentDownload = downloadItem as DownloadItem.TorrentDownload

            binding.apply {
                // Basic info
                tvTitle.text = downloadItem.mediaItem.getDownloadDisplayName()
                tvDownloadType.text = if (torrentDownload.isMagnetLink()) "MAGNET" else "TORRENT"

                // Initialize with model data, will be updated by real-time data
                updateTorrentInfo(torrentDownload)
                updateProgress(torrentDownload)

                // Setup download button widget
                setupDownloadButtonWidget(downloadItem)

                // Long click for remove
                root.setOnLongClickListener {
                    onActionClick(DownloadAction.REMOVE, downloadItem)
                    true
                }
            }
        }

        override fun bindPartial(downloadItem: DownloadItem, payloads: MutableList<Any>) {
            val torrentDownload = downloadItem as DownloadItem.TorrentDownload

            for (payload in payloads) {
                if (payload is List<*>) {
                    for (changeType in payload) {
                        when (changeType) {
                            PAYLOAD_STATUS -> {
                                // Update widget state with real progress from WorkManager
                                val realProgress = realTimeTorrentData["progress"] as? Int
                                    ?: torrentDownload.getProgressPercentage()
                                binding.downloadButtonWidget.updateState(downloadItem.status, realProgress)
                                updateProgress(torrentDownload)
                            }
                            PAYLOAD_PROGRESS -> {
                                // Cache real-time data from WorkManager
                                cacheRealTimeData(downloadItem)

                                // Update UI with real-time data
                                val realProgress = realTimeTorrentData["progress"] as? Int
                                    ?: torrentDownload.getProgressPercentage()
                                binding.downloadButtonWidget.updateState(downloadItem.status, realProgress)
                                updateProgress(torrentDownload)
                                updateTorrentInfo(torrentDownload)
                            }
                        }
                    }
                }
            }
        }

        private fun cacheRealTimeData(downloadItem: DownloadItem) {
            // Extract real-time data from WorkManager progress if available
            // This would typically come from your ViewModel/Repository that observes WorkManager progress
            val torrentDownload = downloadItem as DownloadItem.TorrentDownload

            // For now, we'll use the model data, but this should be replaced with actual WorkManager data
            // You would typically get this from: WorkManager.getInstance().getWorkInfosByTagLiveData(downloadId)
            realTimeTorrentData = mapOf(
                "progress" to torrentDownload.getProgressPercentage(),
                "downloadSpeed" to torrentDownload.downloadSpeed,
                "uploadSpeed" to torrentDownload.uploadSpeed,
                "peers" to torrentDownload.peersConnected,
                "seeds" to torrentDownload.seedsConnected,
                "shareRatio" to torrentDownload.shareRatio,
                "downloadedBytes" to torrentDownload.downloadedBytes,
                "totalBytes" to torrentDownload.fileSize
            )
        }

        private fun updateTorrentInfo(torrentDownload: DownloadItem.TorrentDownload) {
            binding.apply {
                // Use real-time data if available, fallback to model data
                val peers = realTimeTorrentData["peers"] as? Int ?: (torrentDownload.peersConnected ?: 0)
                val seeds = realTimeTorrentData["seeds"] as? Int ?: (torrentDownload.seedsConnected ?: 0)
                val totalPeers = realTimeTorrentData["totalPeers"] as? Int ?: peers
                val shareRatio = realTimeTorrentData["shareRatio"] as? Float ?: (torrentDownload.shareRatio ?: 0.0f)

                // Update peers info with real-time data
                tvPeers.text = "$peers peers, $seeds seeds"

                // Update share ratio with real-time data
                tvShareRatio.text = "Ratio: ${String.format("%.2f", shareRatio)}"
                tvShareRatio.setTextColor(
                    when {
                        shareRatio >= 1.0f -> binding.root.context.getColor(R.color.download_status_completed)
                        shareRatio >= 0.5f -> binding.root.context.getColor(R.color.download_status_downloading)
                        else -> binding.root.context.getColor(R.color.download_status_failed)
                    }
                )

                // For pieces, we'll use model data since it's not in WorkManager progress yet
                tvPieces.text = torrentDownload.getPieceProgress()
            }
        }

        private fun setupDownloadButtonWidget(downloadItem: DownloadItem) {
            val torrentDownload = downloadItem as DownloadItem.TorrentDownload

            binding.downloadButtonWidget.apply {
                // Use real-time progress if available
                val realProgress = realTimeTorrentData["progress"] as? Int
                    ?: torrentDownload.getProgressPercentage()
                updateState(downloadItem.status, realProgress)

                // Set up click handlers based on download status
                onDownloadClick = {
                    onActionClick(DownloadAction.RETRY, downloadItem)
                }

                onPauseClick = {
                    onActionClick(DownloadAction.PAUSE, downloadItem)
                }

                onResumeClick = {
                    onActionClick(DownloadAction.RESUME, downloadItem)
                }

                onCancelClick = {
                    onActionClick(DownloadAction.CANCEL, downloadItem)
                }

                onPlayClick = {
                    onActionClick(DownloadAction.PLAY, downloadItem)
                }
            }
        }

        private fun updateProgress(torrentDownload: DownloadItem.TorrentDownload) {
            binding.apply {
                when (torrentDownload.status) {
                    DownloadStatus.DOWNLOADING -> {
                        // Use real-time data if available
                        val realProgress = realTimeTorrentData["progress"] as? Int
                            ?: torrentDownload.getProgressPercentage()
                        val realDownloadSpeed = realTimeTorrentData["downloadSpeed"] as? Long
                            ?: torrentDownload.downloadSpeed
                        val realUploadSpeed = realTimeTorrentData["uploadSpeed"] as? Long
                            ?: torrentDownload.uploadSpeed
                        val realDownloadedBytes = realTimeTorrentData["downloadedBytes"] as? Long
                            ?: torrentDownload.downloadedBytes
                        val realTotalBytes = realTimeTorrentData["totalBytes"] as? Long
                            ?: torrentDownload.fileSize

                        val progressText = "$realProgress%"
                        val downSpeed = if (realDownloadSpeed > 0) "↓ ${formatSpeed(realDownloadSpeed)}" else "↓ 0 B/s"
                        val upSpeed = if (realUploadSpeed > 0) "↑ ${formatSpeed(realUploadSpeed)}" else "↑ 0 B/s"

                        tvStatus.text = "Downloading • $progressText • ${formatFileSize(realDownloadedBytes)} / ${formatFileSize(realTotalBytes)}"
                        tvSpeeds.text = "$downSpeed • $upSpeed"
                        tvStatus.setTextColor(getStatusColor(torrentDownload.status))
                    }
                    DownloadStatus.PAUSED -> {
                        val realProgress = realTimeTorrentData["progress"] as? Int
                            ?: torrentDownload.getProgressPercentage()
                        tvStatus.text = "Paused • $realProgress%"
                        tvSpeeds.text = ""
                        tvStatus.setTextColor(getStatusColor(torrentDownload.status))
                    }
                    DownloadStatus.COMPLETED -> {
                        val realTotalBytes = realTimeTorrentData["totalBytes"] as? Long
                            ?: torrentDownload.fileSize
                        tvStatus.text = "Completed • ${formatFileSize(realTotalBytes)}"
                        val realUploadSpeed = realTimeTorrentData["uploadSpeed"] as? Long
                            ?: torrentDownload.uploadSpeed
                        val upSpeed = if (realUploadSpeed > 0) "↑ ${formatSpeed(realUploadSpeed)}" else ""
                        tvSpeeds.text = upSpeed
                        tvStatus.setTextColor(getStatusColor(torrentDownload.status))
                    }
                    else -> {
                        tvStatus.text = torrentDownload.status.name.lowercase().replaceFirstChar { it.uppercase() }
                        tvSpeeds.text = ""
                        tvStatus.setTextColor(getStatusColor(torrentDownload.status))
                    }
                }
            }
        }

        private fun formatSpeed(bytesPerSecond: Long): String {
            return when {
                bytesPerSecond >= 1024 * 1024 * 1024 -> "${bytesPerSecond / (1024 * 1024 * 1024)} GB/s"
                bytesPerSecond >= 1024 * 1024 -> "${bytesPerSecond / (1024 * 1024)} MB/s"
                bytesPerSecond >= 1024 -> "${bytesPerSecond / 1024} KB/s"
                else -> "$bytesPerSecond B/s"
            }
        }
    }
}

class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
    override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: DownloadItem, newItem: DownloadItem): Any? {
        val changes = mutableListOf<String>()

//        if (oldItem.status != newItem.status) {
//            changes.add(DownloadsAdapter.PAYLOAD_STATUS)
//        }

        if (oldItem.progress != newItem.progress ||
            oldItem.downloadedBytes != newItem.downloadedBytes ||
            oldItem.downloadSpeed != newItem.downloadSpeed) {
            changes.add(DownloadsAdapter.PAYLOAD_PROGRESS)
        }

        return if (changes.isNotEmpty()) changes else null
    }
}
