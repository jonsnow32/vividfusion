package cloud.app.vvf.ui.download

import android.provider.Settings.Global.getString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.getDownloadDisplayName
import cloud.app.vvf.databinding.ItemDownloadHttpBinding
import cloud.app.vvf.databinding.ItemDownloadHlsBinding
import cloud.app.vvf.databinding.ItemDownloadTorrentBinding
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.DownloadStatus
import cloud.app.vvf.services.downloader.DownloadType
import kotlinx.serialization.Serializable
import java.util.Locale

class DownloadsAdapter(
  private val onActionClick: (DownloadAction, DownloadData) -> Unit
) : ListAdapter<DownloadData, DownloadsAdapter.BaseDownloadViewHolder>(DownloadDiffCallback()) {

  companion object {
    const val VIEW_TYPE_HTTP = 1
    const val VIEW_TYPE_HLS = 2
    const val VIEW_TYPE_TORRENT = 3

    const val PAYLOAD_STATUS = "status"
    const val PAYLOAD_PROGRESS = "progress"
  }

  override fun getItemViewType(position: Int): Int {
    return when (getItem(position).type) {
      DownloadType.HTTP -> VIEW_TYPE_HTTP
      DownloadType.HLS -> VIEW_TYPE_HLS
      DownloadType.TORRENT -> VIEW_TYPE_TORRENT
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

  override fun onBindViewHolder(
    holder: BaseDownloadViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position, payloads)
    } else {
      holder.bindPartial(getItem(position), payloads)
    }
  }

  // Base ViewHolder cho common functionality
  abstract class BaseDownloadViewHolder(
    itemView: View,
    protected val onActionClick: (DownloadAction, DownloadData) -> Unit
  ) : RecyclerView.ViewHolder(itemView) {

    abstract fun bind(data: DownloadData)
    abstract fun bindPartial(data: DownloadData, payloads: MutableList<Any>)

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
    onActionClick: (DownloadAction, DownloadData) -> Unit
  ) : BaseDownloadViewHolder(binding.root, onActionClick) {

    override fun bind(data: DownloadData) {

      binding.apply {
        // Basic info
        tvTitle.text = data.mediaItem?.getDownloadDisplayName() ?: data.fileName ?: data.url
        tvDownloadType.text = "HTTP"

        // HTTP-specific info
        tvConnections.text = "Connections: ${data.connections}"
        tvResumeSupport.text = if (data.resumeSupported) "Resume: Yes" else "Resume: No"
        tvResumeSupport.setTextColor(
          if (data.resumeSupported)
            root.context.getColor(R.color.download_status_completed)
          else
            root.context.getColor(R.color.download_status_cancelled)
        )

        // Status and progress
        updateProgress(data)

        // Setup download button widget
        setupDownloadButtonWidget(data)

        // Long click for remove
        root.setOnLongClickListener {
          onActionClick(DownloadAction.UNKNOW, data)
          true
        }
      }
    }

    override fun bindPartial(data: DownloadData, payloads: MutableList<Any>) {

      for (payload in payloads) {
        if (payload is List<*>) {
          for (changeType in payload) {
            when (changeType) {
              PAYLOAD_STATUS -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
              }

              PAYLOAD_PROGRESS -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
              }
            }
          }
        }
      }
    }

    private fun setupDownloadButtonWidget(data: DownloadData) {

      binding.downloadButtonWidget.apply {
        // Update the widget state
        updateState(data.status, data.progressPercent)

        // Set up click handlers based on download status
        onDownloadClick = {
          onActionClick(DownloadAction.RETRY, data)
        }

        onPauseClick = {
          onActionClick(DownloadAction.PAUSE, data)
        }

        onResumeClick = {
          onActionClick(DownloadAction.RESUME, data)
        }

        onCancelClick = {
          onActionClick(DownloadAction.CANCEL, data)
        }

        onPlayClick = {
          onActionClick(DownloadAction.PLAY, data)
        }
      }
    }

    private fun updateProgress(data: DownloadData) {
      binding.apply {
        when (data.status) {
          DownloadStatus.DOWNLOADING -> {
            val progressText = "${data.progressPercent}%"
            val sizeText =
              "${formatFileSize(data.downloadedBytes)} / ${formatFileSize(data.totalBytes)}"
            val speedText = if (data.downloadSpeed > 0) data.downloadSpeedFormatted else "0 B/s"
            val etaText =
              if (data.downloadSpeed > 0) "ETA: ${data.getEstimatedTimeRemaining()}" else ""

            tvStatus.text = "$progressText • $sizeText"
            tvSpeed.text = speedText
            tvEta.text = etaText
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.PAUSED -> {
            tvStatus.text = "Paused • ${data.progressPercent}%"
            tvSpeed.text = ""
            tvEta.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.COMPLETED -> {
            tvStatus.text = "Completed • ${formatFileSize(data.totalBytes)}"
            tvSpeed.text = ""
            tvEta.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          else -> {
            tvStatus.text = data.status.name.lowercase().replaceFirstChar { it.uppercase() }
            tvSpeed.text = ""
            tvEta.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }
        }
      }
    }
  }

  // HLS Download ViewHolder
  class HlsDownloadViewHolder(
    private val binding: ItemDownloadHlsBinding,
    onActionClick: (DownloadAction, DownloadData) -> Unit
  ) : BaseDownloadViewHolder(binding.root, onActionClick) {

    override fun bind(data: DownloadData) {


      binding.apply {
        // Basic info
        tvTitle.text = data.mediaItem?.getDownloadDisplayName() ?: data.fileName ?: data.url
        tvDownloadType.text = "HLS"

        // HLS-specific info
        tvQuality.text = "Quality: ${data.quality}"
        tvSegments.text = data.getSegmentProgress()
        if (data.encryption != null) {
          tvEncryption.text = "Encrypted: ${data.encryption}"
          tvEncryption.visibility = View.VISIBLE
        } else {
          tvEncryption.visibility = View.GONE
        }

        // Status and progress
        updateProgress(data)

        // Setup download button widget
        setupDownloadButtonWidget(data)

        // Long click for remove
        root.setOnLongClickListener {
          onActionClick(DownloadAction.UNKNOW, data)
          true
        }
      }
    }

    override fun bindPartial(data: DownloadData, payloads: MutableList<Any>) {

      for (payload in payloads) {
        if (payload is List<*>) {
          for (changeType in payload) {
            when (changeType) {
              PAYLOAD_STATUS -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
              }

              PAYLOAD_PROGRESS -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
                binding.tvSegments.text = data.getSegmentProgress()
              }
            }
          }
        }
      }
    }

    private fun setupDownloadButtonWidget(data: DownloadData) {

      binding.downloadButtonWidget.apply {
        // Update the widget state
        updateState(data.status, data.progressPercent)

        // Set up click handlers based on download status
        onDownloadClick = {
          onActionClick(DownloadAction.RETRY, data)
        }

        onPauseClick = {
          onActionClick(DownloadAction.PAUSE, data)
        }

        onResumeClick = {
          onActionClick(DownloadAction.RESUME, data)
        }

        onCancelClick = {
          onActionClick(DownloadAction.CANCEL, data)
        }

        onPlayClick = {
          onActionClick(DownloadAction.PLAY, data)
        }
      }
    }

    private fun updateProgress(data: DownloadData) {
      binding.apply {
        val context = binding.root.context
        when (data.status) {
          DownloadStatus.DOWNLOADING -> {
            val progressText = "${data.progressPercent}%"
            val speedText = if (data.downloadSpeed > 0) data.downloadSpeedFormatted else "0 B/s"
            tvStatus.text =
              context.getString(R.string.download_progress, context.getString(R.string.downloading), progressText)
            tvSpeed.text = speedText
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.PAUSED -> {
            tvStatus.text =  context.getString(R.string.download_progress, context.getString(R.string.paused), "${data.progressPercent}%")
            tvSpeed.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.COMPLETED -> {
            tvStatus.text = context.getString(R.string.download_progress, context.getString(R.string.completed),
              formatFileSize(data.totalBytes)
            )
            tvSpeed.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          else -> {
            tvStatus.text = data.status.name.lowercase().replaceFirstChar { it.uppercase() }
            tvSpeed.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }
        }
      }
    }
  }

  // Torrent Download ViewHolder
  class TorrentDownloadViewHolder(
    private val binding: ItemDownloadTorrentBinding,
    onActionClick: (DownloadAction, DownloadData) -> Unit
  ) : BaseDownloadViewHolder(binding.root, onActionClick) {

    // Cache for real-time torrent data from WorkManager
    private var realTimeTorrentData: Map<String, Any> = emptyMap()

    override fun bind(data: DownloadData) {

      binding.apply {
        // Basic info
        tvTitle.text = data.mediaItem?.getDownloadDisplayName() ?: data.fileName ?: data.url
        tvDownloadType.text = if (data.isTorrentDownload) "MAGNET" else "TORRENT"

        // Initialize with model data, will be updated by real-time data
        updateTorrentInfo(data)
        updateProgress(data)

        // Setup download button widget
        setupDownloadButtonWidget(data)

        // Long click for remove
        root.setOnLongClickListener {
          onActionClick(DownloadAction.UNKNOW, data)
          true
        }
      }
    }

    override fun bindPartial(data: DownloadData, payloads: MutableList<Any>) {

      for (payload in payloads) {
        if (payload is List<*>) {
          for (changeType in payload) {
            when (changeType) {
              PAYLOAD_STATUS -> {
                // Update widget state with real progress from WorkManager
                val realProgress = realTimeTorrentData["progress"] as? Int
                  ?: data.progressPercent
                binding.downloadButtonWidget.updateState(data.status, realProgress)
                updateProgress(data)
              }

              PAYLOAD_PROGRESS -> {
                // Cache real-time data from WorkManager
                cacheRealTimeData(data)

                // Update UI with real-time data
                val realProgress = realTimeTorrentData["progress"] as? Int
                  ?: data.progressPercent
                binding.downloadButtonWidget.updateState(data.status, realProgress)
                updateProgress(data)
                updateTorrentInfo(data)
              }
            }
          }
        }
      }
    }

    private fun cacheRealTimeData(data: DownloadData) {

      // For now, we'll use the model data, but this should be replaced with actual WorkManager data
      // You would typically get this from: WorkManager.getInstance().getWorkInfosByTagLiveData(downloadId)
      realTimeTorrentData = mapOf(
        "progress" to data.progressPercent,
        "downloadSpeed" to data.downloadSpeed,
        "uploadSpeed" to data.uploadSpeed,
        "peers" to data.totalPeers,
        "seeds" to data.seeds,
        "shareRatio" to data.shareRatio,
        "downloadedBytes" to data.downloadedBytes,
        "totalBytes" to data.totalBytes
      )
    }

    private fun updateTorrentInfo(data: DownloadData) {
      binding.apply {
        // Use real-time data if available, fallback to model data
        val peers = realTimeTorrentData["peers"] as? Int ?: data.peers
        val seeds = realTimeTorrentData["seeds"] as? Int ?: data.seeds
        val totalPeers = realTimeTorrentData["totalPeers"] as? Int ?: peers
        val shareRatio = realTimeTorrentData["shareRatio"] as? Float ?: data.shareRatio

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
        tvPieces.text = "data.getPieceProgress()"
      }
    }

    private fun setupDownloadButtonWidget(data: DownloadData) {

      binding.downloadButtonWidget.apply {
        // Use real-time progress if available
        val realProgress = realTimeTorrentData["progress"] as? Int
          ?: data.progressPercent
        updateState(data.status, realProgress)

        // Set up click handlers based on download status
        onDownloadClick = {
          onActionClick(DownloadAction.RETRY, data)
        }

        onPauseClick = {
          onActionClick(DownloadAction.PAUSE, data)
        }

        onResumeClick = {
          onActionClick(DownloadAction.RESUME, data)
        }

        onCancelClick = {
          onActionClick(DownloadAction.CANCEL, data)
        }

        onPlayClick = {
          onActionClick(DownloadAction.PLAY, data)
        }
      }
    }

    private fun updateProgress(data: DownloadData) {
      binding.apply {
        when (data.status) {
          DownloadStatus.DOWNLOADING -> {
            // Use real-time data if available
            val realProgress = realTimeTorrentData["progress"] as? Int
              ?: data.progressPercent
            val realDownloadSpeed = realTimeTorrentData["downloadSpeed"] as? Long
              ?: data.downloadSpeed
            val realUploadSpeed = realTimeTorrentData["uploadSpeed"] as? Long
              ?: data.uploadSpeed
            val realDownloadedBytes = realTimeTorrentData["downloadedBytes"] as? Long
              ?: data.downloadedBytes
            val realTotalBytes = realTimeTorrentData["totalBytes"] as? Long
              ?: data.totalBytes

            val progressText = "$realProgress%"
            val downSpeed =
              if (realDownloadSpeed > 0) "↓ ${formatSpeed(realDownloadSpeed)}" else "↓ 0 B/s"
            val upSpeed =
              if (realUploadSpeed > 0) "↑ ${formatSpeed(realUploadSpeed)}" else "↑ 0 B/s"

            tvStatus.text =
              "Downloading • $progressText • ${formatFileSize(realDownloadedBytes)} / ${
                formatFileSize(realTotalBytes)
              }"
            tvSpeeds.text = "$downSpeed • $upSpeed"
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.PAUSED -> {
            val realProgress = realTimeTorrentData["progress"] as? Int
              ?: data.progressPercent
            tvStatus.text = "Paused • $realProgress%"
            tvSpeeds.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.COMPLETED -> {
            val realTotalBytes = realTimeTorrentData["totalBytes"] as? Long
              ?: data.totalBytes
            tvStatus.text = "Completed • ${formatFileSize(realTotalBytes)}"
            val realUploadSpeed = realTimeTorrentData["uploadSpeed"] as? Long
              ?: data.uploadSpeed
            val upSpeed = if (realUploadSpeed > 0) "↑ ${formatSpeed(realUploadSpeed)}" else ""
            tvSpeeds.text = upSpeed
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          else -> {
            tvStatus.text = data.status.name.lowercase().replaceFirstChar { it.uppercase() }
            tvSpeeds.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
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

class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadData>() {
  override fun areItemsTheSame(oldItem: DownloadData, newItem: DownloadData): Boolean {
    return oldItem.id == newItem.id
  }

  override fun areContentsTheSame(oldItem: DownloadData, newItem: DownloadData): Boolean {
    return oldItem == newItem
  }

  override fun getChangePayload(oldItem: DownloadData, newItem: DownloadData): Any? {
    val changes = mutableListOf<String>()

//        if (oldItem.status != newItem.status) {
//            changes.add(DownloadsAdapter.PAYLOAD_STATUS)
//        }

    if (oldItem.progress != newItem.progress ||
      oldItem.downloadedBytes != newItem.downloadedBytes ||
      oldItem.downloadSpeed != newItem.downloadSpeed
    ) {
      changes.add(DownloadsAdapter.PAYLOAD_PROGRESS)
    }

    return if (changes.isNotEmpty()) changes else null
  }
}

@Serializable
enum class DownloadAction {
  CANCEL,
  PAUSE,
  RESUME,
  RETRY,
  PLAY,
  REMOVE,

  //name of action that goto the file location in local storage
  LOCALTION,

  UNKNOW,

}
