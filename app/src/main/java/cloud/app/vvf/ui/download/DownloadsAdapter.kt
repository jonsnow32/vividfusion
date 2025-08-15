package cloud.app.vvf.ui.download

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.databinding.ItemDownloadHttpBinding
import cloud.app.vvf.databinding.ItemDownloadHlsBinding
import cloud.app.vvf.databinding.ItemDownloadTorrentBinding
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.DownloadStatus
import cloud.app.vvf.services.downloader.DownloadType
import cloud.app.vvf.utils.toHumanReadableSize
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
    const val PAYLOAD_CONTENT = "content"
  }

  override fun getItemViewType(position: Int): Int {
    return when (getItem(position).type) {
      DownloadType.HTTP -> VIEW_TYPE_HTTP
      DownloadType.HLS -> VIEW_TYPE_HLS
      DownloadType.TORRENT -> VIEW_TYPE_TORRENT
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseDownloadViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      VIEW_TYPE_HTTP -> {
        val binding = ItemDownloadHttpBinding.inflate(inflater, parent, false)
        HttpDownloadViewHolder(binding, onActionClick)
      }
      VIEW_TYPE_HLS -> {
        val binding = ItemDownloadHlsBinding.inflate(inflater, parent, false)
        HlsDownloadViewHolder(binding, onActionClick)
      }
      VIEW_TYPE_TORRENT -> {
        val binding = ItemDownloadTorrentBinding.inflate(inflater, parent, false)
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

  // Base ViewHolder for common functionality
  abstract class BaseDownloadViewHolder(
    itemView: View,
    protected val onActionClick: (DownloadAction, DownloadData) -> Unit
  ) : RecyclerView.ViewHolder(itemView) {

    // Cache for frequently accessed values
    protected val context = itemView.context

    // Store current data to ensure click handlers always use latest data
    protected var currentData: DownloadData? = null

    abstract fun bind(data: DownloadData)
    abstract fun bindPartial(data: DownloadData, payloads: MutableList<Any>)

    protected fun getStatusColor(status: DownloadStatus): Int {
      return when (status) {
        DownloadStatus.COMPLETED -> context.getColor(R.color.download_status_completed)
        DownloadStatus.DOWNLOADING -> context.getColor(R.color.download_status_downloading)
        DownloadStatus.FAILED -> context.getColor(R.color.download_status_failed)
        DownloadStatus.CANCELLED -> context.getColor(R.color.download_status_cancelled)
        else -> context.getColor(R.color.download_status_default)
      }
    }

    // Optimized file size formatting with caching
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

    // Common progress update logic
    protected fun getProgressText(data: DownloadData): String {
      return when (data.status) {
        DownloadStatus.DOWNLOADING -> {
          val progressText = "${data.progressPercent}%"
          val sizeText = "${formatFileSize(data.downloadedBytes)} / ${formatFileSize(data.totalBytes)}"
          val speedText = if (data.downloadSpeed > 0) data.downloadSpeedFormatted else "0 B/s"
          "$progressText • $sizeText • $speedText"
        }
        DownloadStatus.PAUSED -> "Paused • ${data.progressPercent}%"
        DownloadStatus.COMPLETED -> "Completed • ${formatFileSize(data.totalBytes)}"
        else -> data.status.name.lowercase().replaceFirstChar { it.uppercase() }
      }
    }

    // Common ETA calculation
    protected fun getEtaText(data: DownloadData): String {
      return if (data.status == DownloadStatus.DOWNLOADING && data.downloadSpeed > 0) {
        "ETA: ${data.getEstimatedTimeRemaining()}"
      } else ""
    }

    // Common setup for download button widget
    protected fun setupDownloadButtonWidget(downloadButtonWidget: Any) {
      if (downloadButtonWidget is DownloadButtonWidget) {
        downloadButtonWidget.apply {
          onDownloadClick = { currentData?.let { onActionClick(DownloadAction.RETRY, it) } }
          onPauseClick = { currentData?.let { onActionClick(DownloadAction.PAUSE, it) } }
          onResumeClick = { currentData?.let { onActionClick(DownloadAction.RESUME, it) } }
          onCancelClick = { currentData?.let { onActionClick(DownloadAction.CANCEL, it) } }
          onPlayClick = { currentData?.let { onActionClick(DownloadAction.PLAY, it) } }

          currentData?.let { data ->
            updateState(data.status, data.progressPercent)
          }
        }
      }
    }

    // Common long click setup
    protected fun setupLongClick(view: View) {
      view.setOnLongClickListener {
        currentData?.let { onActionClick(DownloadAction.UNKNOW, it) }
        true
      }
    }
  }

  // HTTP Download ViewHolder
  class HttpDownloadViewHolder(
    private val binding: ItemDownloadHttpBinding,
    onActionClick: (DownloadAction, DownloadData) -> Unit
  ) : BaseDownloadViewHolder(binding.root, onActionClick) {

    override fun bind(data: DownloadData) {
      currentData = data

      binding.apply {
        // Basic info
        tvTitle.text =  data.title ?: data.url

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
        setupDownloadButtonWidget(binding.downloadButtonWidget)

        // Long click for remove
        setupLongClick(root)
      }
    }

    override fun bindPartial(data: DownloadData, payloads: MutableList<Any>) {
      // Update current data reference
      currentData = data

      for (payload in payloads) {
        if (payload is List<*>) {
          for (changeType in payload) {
            when (changeType) {
              PAYLOAD_STATUS -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
              }

              PAYLOAD_CONTENT -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                binding.tvTitle.text = data.getDisplayName()
                updateProgress(data)
              }
            }
          }
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
            tvConnections.text = "Connections: ${data.connections}"
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
      currentData = data

      binding.apply {
        // Basic info
        tvTitle.text =  data.title ?: data.url

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
        setupDownloadButtonWidget(binding.downloadButtonWidget)
        // Long click for remove
        setupLongClick(root)
      }
    }

    override fun bindPartial(data: DownloadData, payloads: MutableList<Any>) {
      // Update current data reference
      currentData = data

      for (payload in payloads) {
        if (payload is List<*>) {
          for (changeType in payload) {
            when (changeType) {
              PAYLOAD_STATUS -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
              }

              PAYLOAD_CONTENT -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
                binding.tvSegments.text = data.getSegmentProgress()
                binding.tvTitle.text = data.getDisplayName()
                binding.tvQuality.text = binding.root.context.getString(R.string.quality, data.quality)
              }
            }
          }
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
              context.getString(
                R.string.download_progress,
                context.getString(R.string.downloading),
                progressText
              )
            tvSpeed.text = speedText
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.PAUSED -> {
            tvStatus.text = context.getString(
              R.string.download_progress,
              context.getString(R.string.paused),
              "${data.progressPercent}%"
            )
            tvSpeed.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }

          DownloadStatus.COMPLETED -> {
            tvStatus.text = context.getString(
              R.string.download_progress, context.getString(R.string.completed),
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

        // Update file size display
        tvFileSize.text = if (data.totalBytes > 0) {
          formatFileSize(data.totalBytes)
        } else {
          "Unknown"
        }
      }
    }
  }

  // Torrent Download ViewHolder
  class TorrentDownloadViewHolder(
    private val binding: ItemDownloadTorrentBinding,
    onActionClick: (DownloadAction, DownloadData) -> Unit
  ) : BaseDownloadViewHolder(binding.root, onActionClick) {

    override fun bind(data: DownloadData) {
      currentData = data

      binding.apply {
        // Basic info
        tvTitle.text =  data.title ?: data.url

        // Directly use DownloadData for info
        updateTorrentInfo(data)
        updateProgress(data)

        // Setup download button widget
        setupDownloadButtonWidget(binding.downloadButtonWidget)

        // Long click for remove
        setupLongClick(root)
      }
    }

    override fun bindPartial(data: DownloadData, payloads: MutableList<Any>) {
      // Update current data reference
      currentData = data

      for (payload in payloads) {
        if (payload is List<*>) {
          for (changeType in payload) {
            when (changeType) {
              PAYLOAD_STATUS, PAYLOAD_CONTENT -> {
                binding.downloadButtonWidget.updateState(data.status, data.progressPercent)
                updateProgress(data)
                updateTorrentInfo(data)
              }
            }
          }
        }
      }
    }

    private fun updateTorrentInfo(data: DownloadData) {
      binding.apply {
        val peers = data.peers
        val seeds = data.seeds
        tvPeers.text = binding.root.context.getString(R.string.peers_seeds, peers, seeds)
        binding.tvTitle.text = data.getDisplayName()
      }
    }

    private fun updateProgress(data: DownloadData) {
      binding.apply {
        when (data.status) {
          DownloadStatus.DOWNLOADING -> {
            val progressText = "${data.progressPercent}%"
            val downSpeed = if (data.downloadSpeed > 0) "↓ ${formatSpeed(data.downloadSpeed)}" else "↓ 0 B/s"
            val upSpeed = if (data.uploadSpeed > 0) "↑ ${formatSpeed(data.uploadSpeed)}" else "↑ 0 B/s"
            tvStatus.text = "Downloading • $progressText • ${formatFileSize(data.downloadedBytes)} / ${formatFileSize(data.totalBytes)}"
            tvSpeeds.text = "$downSpeed • $upSpeed"
            tvStatus.setTextColor(getStatusColor(data.status))
          }
          DownloadStatus.PAUSED -> {
            tvStatus.text = "Paused • ${data.progressPercent}%"
            tvSpeeds.text = ""
            tvStatus.setTextColor(getStatusColor(data.status))
          }
          DownloadStatus.COMPLETED -> {
            tvStatus.text = "Completed �� ${formatFileSize(data.totalBytes)}"
            val upSpeed = if (data.uploadSpeed > 0) "↑ ${formatSpeed(data.uploadSpeed)}" else ""
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
    // Compare only the relevant fields that affect UI display, excluding updatedAt
    return oldItem.status == newItem.status &&
        oldItem.progress == newItem.progress &&
        oldItem.downloadedBytes == newItem.downloadedBytes &&
        oldItem.totalBytes == newItem.totalBytes &&
        oldItem.downloadSpeed == newItem.downloadSpeed &&
        oldItem.title == newItem.title &&
        oldItem.filePath == newItem.filePath &&
        oldItem.segmentsDownloaded == newItem.segmentsDownloaded &&
        oldItem.quality == newItem.quality &&
        oldItem.typeSpecificData == newItem.typeSpecificData
  }

  override fun getChangePayload(oldItem: DownloadData, newItem: DownloadData): Any? {
    val changes = mutableListOf<String>()

    if (oldItem.status != newItem.status) {
      changes.add(DownloadsAdapter.PAYLOAD_STATUS)
    }

    if (oldItem.progress != newItem.progress ||
      oldItem.downloadedBytes != newItem.downloadedBytes ||
      oldItem.downloadSpeed != newItem.downloadSpeed ||
      oldItem.quality != newItem.quality ||
      oldItem.title != newItem.title ||
      oldItem.segmentsDownloaded != newItem.segmentsDownloaded
    ) {
      changes.add(DownloadsAdapter.PAYLOAD_CONTENT)
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
