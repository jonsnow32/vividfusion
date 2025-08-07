package cloud.app.vvf.ui.download

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import cloud.app.vvf.R
import cloud.app.vvf.services.downloader.DownloadStatus

class DownloadButtonWidget @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  private val btnDownloadAction: ImageButton
  private val progressCircle: ProgressBar
  private val progressBackground: android.view.View

  var onDownloadClick: (() -> Unit)? = null
  var onPauseClick: (() -> Unit)? = null
  var onResumeClick: (() -> Unit)? = null
  var onCancelClick: (() -> Unit)? = null
  var onPlayClick: (() -> Unit)? = null

  init {
    LayoutInflater.from(context).inflate(R.layout.widget_download_button, this, true)

    btnDownloadAction = findViewById(R.id.btn_download_action)
    progressCircle = findViewById(R.id.progress_circle)
    progressBackground = findViewById(R.id.view_progress_background)

    btnDownloadAction.setOnClickListener {
      handleButtonClick()
    }
  }

  private var currentStatus: DownloadStatus? = null
  private var currentProgress: Int = 0

  fun updateState(downloadStatus: DownloadStatus?, progress: Int) {
    currentStatus = downloadStatus
    currentProgress = progress

    // Update progress circle
    when (downloadStatus) {
      DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED -> {
        progressBackground.visibility = VISIBLE
        progressCircle.visibility = VISIBLE
        progressCircle.progress = progress
      }

      else -> {
        progressBackground.visibility = GONE
        progressCircle.visibility = GONE
      }
    }

    // Update button icon
    val iconRes = when (downloadStatus) {
      null, DownloadStatus.CANCELLED -> R.drawable.outline_rotate_left_24
      DownloadStatus.PENDING, DownloadStatus.DOWNLOADING -> R.drawable.ic_pause_24
      DownloadStatus.PAUSED -> R.drawable.resume_24dp
      DownloadStatus.COMPLETED -> R.drawable.ic_play_circle
      DownloadStatus.FAILED -> R.drawable.outline_rotate_left_24
    }

    btnDownloadAction.setImageResource(iconRes)
    btnDownloadAction.contentDescription = getContentDescription(downloadStatus)
  }

  private fun handleButtonClick() {
    when (currentStatus) {
      null -> onDownloadClick?.invoke()
      DownloadStatus.PENDING, DownloadStatus.DOWNLOADING -> onPauseClick?.invoke()
      DownloadStatus.PAUSED -> onResumeClick?.invoke()
      DownloadStatus.FAILED -> onDownloadClick?.invoke()
      DownloadStatus.COMPLETED -> onPlayClick?.invoke()
      DownloadStatus.CANCELLED -> onDownloadClick?.invoke()
    }
  }

  private fun getContentDescription(status: DownloadStatus?): String {
    return when (status) {
      null -> "Start download"
      DownloadStatus.PENDING -> "Download pending"
      DownloadStatus.DOWNLOADING -> "Pause download"
      DownloadStatus.PAUSED -> "Resume download"
      DownloadStatus.COMPLETED -> "Play downloaded file"
      DownloadStatus.FAILED -> "Retry download"
      DownloadStatus.CANCELLED -> "Start download"
    }
  }
}
