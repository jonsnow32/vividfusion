package cloud.app.vvf.ui.detail

import android.content.ContextWrapper
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import cloud.app.vvf.MainActivityViewModel
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
import cloud.app.vvf.common.utils.secondsToReadable
import cloud.app.vvf.datastore.app.helper.BookmarkItem
import cloud.app.vvf.ui.widget.dialog.SelectionDialog


fun ImageView.loadWith(imageHolder: ImageHolder?) {
  if (imageHolder == null) return
  imageHolder.loadInto(this)
}

fun LayoutMediaHeaderBinding.bind(mediaItem: AVPMediaItem, fragment: Fragment, extras: Map<String, String>? = null) {

  val mainViewModel by fragment.activityViewModels<MainActivityViewModel>()
  val context = fragment.context ?: return

  when (mediaItem) {
    is AVPMediaItem.SeasonItem -> imageBackdrop.loadWith(mediaItem.season.generalInfo.backdrop?.toImageHolder())
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
  textOverview.setTextWithVisibility(
    if (mediaItem.generalInfo?.overview.isNullOrEmpty()) getString(
      textOverview.context,
      R.string.no_overview_metada
    ) else mediaItem.generalInfo?.overview
  )
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
  textRatingCount.setTextWithVisibility(if(mediaItem.generalInfo?.voteCount != null) "/${mediaItem.generalInfo?.voteCount}" else null)
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

  dot.isGone = mediaItem.generalInfo?.voteCount == null || mediaItem.releaseMonthYear == null
  dot2.isGone = mediaItem.generalInfo?.runtime == null
  runTime.setTextWithVisibility(secondsToReadable(mediaItem.generalInfo?.runtime))

  if (mediaItem.homePage.isNullOrEmpty()) {
    imgBtnHomepage.isGone = true;
  } else {
    imgBtnHomepage.isGone = false
    imgBtnHomepage.setOnClickListener {
      Utils.launchWebsite(this.root.context, mediaItem.homePage!!)
    }
  }

  if (mediaItem is AVPMediaItem.ShowItem) {
    dot3.isGone = mediaItem.show.contentRating == null
    contentRating.setTextWithVisibility(mediaItem.show.contentRating)
  } else {
    dot3.isGone = true
    contentRating.isGone = true
  }

  val activity = when (val context = this.root.context) {
    is FragmentActivity -> context
    is ContextWrapper -> (context.baseContext as? FragmentActivity)
    else -> null
  }
  val backStackCount = activity?.supportFragmentManager?.backStackEntryCount ?: 0
  if (backStackCount > 3) {
    imgBtnHome.isGone = false
    imgBtnHome.setOnClickListener {
      activity?.let {
        val fragmentManager = it.supportFragmentManager
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fragmentManager.commit {
          setCustomAnimations(0, 0)
          replace(R.id.navHostFragment, MainFragment())
        }
      }
    }

  } else {
    imgBtnHome.isGone = true;
  }

  buttonBookmark.setOnClickListener {
    val status = mainViewModel.getBookmark(mediaItem)
    val bookmarks = BookmarkItem.getBookmarkItemSubclasses().toMutableList().apply {
      add("None")
    }
    val selectedIndex =
      if (status == null) (bookmarks.size - 1) else bookmarks.indexOf(status.javaClass.simpleName);

    SelectionDialog.single(
      bookmarks, selectedIndex,
      context.getString(R.string.add_to_bookmark),
      false
    ).show(fragment.parentFragmentManager)
    { result ->
      result?.let {
        val items = result.getIntegerArrayList("selected_items")
        if (!items.isNullOrEmpty()) {
          mainViewModel.addToBookmark(mediaItem, bookmarks[items[0]]);

          val bookmarkStatus = mainViewModel.getBookmark(mediaItem)
          buttonBookmark.setText(BookmarkItem.getStringIds(bookmarkStatus))
          if (bookmarkStatus != null) {
            buttonBookmark.icon =
              ContextCompat.getDrawable(fragment.requireActivity(), R.drawable.ic_bookmark_filled)
          }
        }
      }
    }
  }
  val bookmarkStatus = mainViewModel.getBookmark(mediaItem)
  buttonBookmark.setText(BookmarkItem.getStringIds(bookmarkStatus))
  if (bookmarkStatus != null) {
    buttonBookmark.icon =
      ContextCompat.getDrawable(context, R.drawable.ic_bookmark_filled)
  }


 if(mediaItem is AVPMediaItem.ShowItem) {
   dot4.isGone = mediaItem.show.status.isNullOrEmpty()
   status.setTextWithVisibility(mediaItem.show.status)
 }

  imgBtnShowNotify.isGone = true
  buttonShowComments.isGone = true
  buttonCollections.isGone = true
  buttonLastWatchedEpisode.isGone = true
  buttonShowTrailer.isGone = mediaItem.generalInfo?.videos.isNullOrEmpty()

  buttonStreamingSearch.isGone = (mediaItem is AVPMediaItem.ShowItem || mediaItem is AVPMediaItem.SeasonItem)
  buttonShowTrailer.isGone = mediaItem.generalInfo?.videos.isNullOrEmpty()

}

