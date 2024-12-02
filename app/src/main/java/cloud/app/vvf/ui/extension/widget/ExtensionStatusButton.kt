package cloud.app.vvf.ui.extension.widget


import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import cloud.app.vvf.R

enum class InstallStatus {
  NOT_INSTALL, INSTALLED, DOWNLOADING, PAUSED, INSTALLING, FAILED, CANCELED
}

class ExtensionStatusButton @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  private val downloadIcon: ImageView
  private val downloadProgress: ProgressBar

  init {
    // Create a container layout
    val container = FrameLayout(context)

    // Initialize ImageView and ProgressBar
    downloadIcon = ImageView(context).apply {
      id = R.id.downloadIcon
      layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
        gravity = Gravity.CENTER
      }
    }

    downloadProgress = ProgressBar(context).apply {
      id = R.id.downloadProgress
      layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
        gravity = Gravity.CENTER
      }
      visibility = View.GONE // initially hidden
    }

    // Add views to the container layout
    container.addView(downloadIcon)
    container.addView(downloadProgress)
    // Read the custom attribute value
    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DownloadButton)
    val downloadStatusValue = typedArray.getInt(R.styleable.DownloadButton_downloadStatus, 0)
    val status = InstallStatus.entries[downloadStatusValue]
    setDownloadStatus(status)
    typedArray.recycle()

    // Add the container layout as the button's content
    addView(container)
  }

  /**
   * Update the button based on the current download status
   */
  fun setDownloadStatus(status: InstallStatus) {
    when (status) {
      InstallStatus.CANCELED,
      InstallStatus.NOT_INSTALL -> {
        downloadIcon.setImageResource(R.drawable.download_2_24dp)
        downloadProgress.visibility = View.GONE
      }

      InstallStatus.INSTALLING,
      InstallStatus.DOWNLOADING -> {
        downloadIcon.setImageResource(R.drawable.pause_24dp)
        downloadProgress.visibility = View.VISIBLE
      }

      InstallStatus.PAUSED -> {
        downloadIcon.setImageResource(R.drawable.resume_24dp)
        downloadProgress.visibility = View.VISIBLE
      }

      InstallStatus.INSTALLED -> {
        downloadIcon.setImageResource(R.drawable.ic_delete)
        downloadProgress.visibility = View.GONE
      }

      InstallStatus.FAILED -> {
        downloadIcon.setImageResource(R.drawable.error_24dp)
        downloadProgress.visibility = View.GONE
      }
    }
  }

  /**
   * Update the download progress
   */
  fun setDownloadProgress(progress: Int) {
    downloadProgress.progress = progress
  }
}

