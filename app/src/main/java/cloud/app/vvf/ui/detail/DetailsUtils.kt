package cloud.app.vvf.ui.detail

import android.content.ContextWrapper
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getString
import androidx.core.view.isGone
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import cloud.app.vvf.R
import cloud.app.vvf.databinding.LayoutMediaHeaderBinding
import cloud.app.vvf.ui.main.MainFragment
import cloud.app.vvf.utils.Utils
import cloud.app.vvf.utils.loadInto
import cloud.app.vvf.utils.roundTo
import cloud.app.vvf.utils.setTextWithVisibility
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder


fun ImageView.loadWith(imageHolder: ImageHolder?) {
  if (imageHolder == null) return
  imageHolder.loadInto(this)
}

fun LayoutMediaHeaderBinding.bind(mediaItem: AVPMediaItem, extras: Map<String, String>? = null) {

  when (mediaItem) {
    is AVPMediaItem.SeasonItem -> imageBackdrop.loadWith(mediaItem.season.backdrop?.toImageHolder())
    else -> imageBackdrop.loadWith(mediaItem.backdrop)
  }

  imagePoster.loadWith(mediaItem.poster)

  textTitle.setTextWithVisibility(mediaItem.title)
  if (mediaItem is AVPMediaItem.SeasonItem) {
    val progress = textSubtitle.context.getString(
      R.string.season_progress_format,
      mediaItem.watchedEpisodeNumber ?: 0,
      mediaItem.season.episodeCount
    )
    textSubtitle.setTextWithVisibility(progress)
  } else textSubtitle.setTextWithVisibility(mediaItem.subtitle)
  textOverview.setTextWithVisibility(mediaItem.overview ?: "")
  textReleaseYear.setTextWithVisibility((mediaItem.releaseYear ?: "").toString())
  textRating.setTextWithVisibility(mediaItem.rating?.toString() ?: "")

  textReleaseYear.setTextWithVisibility(mediaItem.releaseMonthYear)
  val format = getString(this.imageBackdrop.context, R.string.rating_format)
  textRating.setTextWithVisibility(
    if (mediaItem.rating != null) String.format(
      format,
      mediaItem.rating?.roundTo(1)
    ) else null
  )
  textOverview.setOnClickListener {
    val dialogView =
      LayoutInflater.from(textOverview.context).inflate(R.layout.dialog_full_text, null)
    val dialogTextView = dialogView.findViewById<TextView>(R.id.textFullContent)
    dialogTextView.text = textOverview.text

    AlertDialog.Builder(textOverview.context)
      .setView(dialogView)
      .setPositiveButton("Close", null)
      .show()
  }
  if (mediaItem.homePage.isNullOrEmpty()) {
    showOpenInBrowser.isGone = true;
  } else {
    showOpenInBrowser.isGone = false
    showOpenInBrowser.setOnClickListener {
      Utils.launchWebsite(this.root.context, mediaItem.homePage!!)
    }

  }

  val activity = when (val context = this.root.context) {
    is FragmentActivity -> context
    is ContextWrapper -> (context.baseContext as? FragmentActivity)
    else -> null
  }
  val backStackCount = activity?.supportFragmentManager?.backStackEntryCount ?: 0
  if(backStackCount > 3) {
    homeButton.isGone = false
    homeButton.setOnClickListener {
      activity?.let {
        val fragmentManager = it.supportFragmentManager
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fragmentManager.commit {
          setCustomAnimations(0,0)
          replace(R.id.navHostFragment, MainFragment())
        }
      }
    }
  }
}

