package cloud.app.avp.ui.detail

import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getString
import cloud.app.avp.R
import cloud.app.avp.databinding.LayoutMediaHeaderBinding
import cloud.app.avp.utils.TimeUtils.toLocalMonthYear
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.roundTo
import cloud.app.avp.utils.setTextWithVisibility
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.ImageHolder


fun ImageView.loadWith(imageHolder: ImageHolder?){
  if(imageHolder == null) return
  imageHolder.loadInto(this)
}

fun LayoutMediaHeaderBinding.bind(mediaItem: AVPMediaItem){
  imageBackdrop.loadWith(mediaItem.backdrop)
  imagePoster.loadWith(mediaItem.poster)

  textTitle.setTextWithVisibility(mediaItem.title)
  textSubtitle.setTextWithVisibility(mediaItem.subtitle)
  textOverview.setTextWithVisibility(mediaItem.overview ?: "")
  textReleaseYear.setTextWithVisibility((mediaItem.releaseYear ?: "").toString())
  textRating.setTextWithVisibility(mediaItem.rating?.toString() ?: "")

  textReleaseYear.setTextWithVisibility(mediaItem.generalInfo?.releaseDateMsUTC?.toLocalMonthYear())
  val format = getString(this.imageBackdrop.context,  R.string.rating_format)
  textRating.setTextWithVisibility(String.format(format, mediaItem.rating?.roundTo(1)))

  textOverview.setOnClickListener {
    val dialogView = LayoutInflater.from(textOverview.context).inflate(R.layout.dialog_full_text, null)
    val dialogTextView = dialogView.findViewById<TextView>(R.id.textFullContent)
    dialogTextView.text = textOverview.text

    AlertDialog.Builder(textOverview.context)
      .setView(dialogView)
      .setPositiveButton("Close", null)
      .show()
  }
}
