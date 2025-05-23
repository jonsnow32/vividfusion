package cloud.app.vvf.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.utils.millisecondsToReadable
import cloud.app.vvf.databinding.ItemActorBinding
import cloud.app.vvf.databinding.ItemMediaBinding
import cloud.app.vvf.databinding.ItemMediaTitleBinding
import cloud.app.vvf.databinding.ItemMediaCoverBinding
import cloud.app.vvf.databinding.ItemSeasonBinding
import cloud.app.vvf.databinding.ItemSeasonLargeBinding
import cloud.app.vvf.databinding.ItemTrackBinding
import cloud.app.vvf.databinding.ItemVideoBinding
import cloud.app.vvf.utils.loadInto
import cloud.app.vvf.utils.roundTo
import cloud.app.vvf.utils.setTextWithVisibility
import com.uwetrottmann.tmdb2.entities.FavoriteMedia

sealed class MediaItemViewHolder(itemView: View) :
  RecyclerView.ViewHolder(itemView) {
  abstract fun bind(item: AVPMediaItem)
  abstract val transitionView: View

  fun ItemMediaTitleBinding.bind(item: AVPMediaItem) {
    title.text = item.title
    subtitle.isVisible = item.subtitle.isNullOrEmpty().not()
    subtitle.text = item.subtitle
  }

  fun ItemMediaCoverBinding.bind(item: AVPMediaItem) {
    when (item) {
      is AVPMediaItem.PlaybackProgress -> {
        item.poster.loadInto(imageView, item.placeHolder())
        this.iconContainer.isVisible = item.rating != null
        this.rating.text = item.rating?.roundTo(1).toString()
        if (item.duration != null) {
          watchProgress.visibility = View.VISIBLE
          watchProgress.progress = ((item.position.toDouble() / item.duration!!) * 100).toInt()
        }
        this.playIcon.visibility = View.VISIBLE
      }

      is AVPMediaItem.TrackItem -> {
        item.poster.loadInto(imageView, item.placeHolder())
        this.iconContainer.isVisible = true
        this.rating.setTextWithVisibility(item.track.duration?.millisecondsToReadable())
//        this.playIcon.visibility = View.VISIBLE
      }

        is AVPMediaItem.VideoItem -> {
        item.poster.loadInto(imageView, item.placeHolder())
        this.iconContainer.isVisible = true
        this.rating.setTextWithVisibility(item.video.duration?.millisecondsToReadable())
//        this.playIcon.visibility = View.VISIBLE
      }

      is AVPMediaItem.ActorItem,
      is AVPMediaItem.EpisodeItem,
      is AVPMediaItem.VideoCollectionItem,
      is AVPMediaItem.MovieItem,
      is AVPMediaItem.SeasonItem,
      is AVPMediaItem.ShowItem,
      //is AVPMediaItem.StreamItem,
      is AVPMediaItem.TrailerItem -> {
        item.poster.loadInto(imageView, item.placeHolder())
        this.iconContainer.isVisible = item.rating != null
        this.rating.text = item.rating?.roundTo(1).toString()
      }
    }

  }

  class Media(val binding: ItemMediaBinding) : MediaItemViewHolder(binding.root) {
    private val titleBinding = ItemMediaTitleBinding.bind(binding.root)
    override val transitionView: View
      get() = binding.cover.imageView

    override fun bind(item: AVPMediaItem) {
      titleBinding.bind(item)
      binding.cover.bind(item)
    }

    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Media(
          ItemMediaBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }


  class SeasonLarge(val binding: ItemSeasonLargeBinding) : MediaItemViewHolder(binding.root) {
    override val transitionView: View
      get() = binding.root

    override fun bind(item: AVPMediaItem) {
      val season = (item as AVPMediaItem.SeasonItem).season
      binding.seasonTitle.text = season.generalInfo.title
      binding.watchProgress.text = binding.root.context.resources.getString(
        R.string.season_progress_format,
        item.watchedEpisodeNumber ?: 0,
        season.episodeCount
      )
      binding.seasonProgress.progress = if (season.episodeCount > 0) ((item.watchedEpisodeNumber
        ?: 0) * 100 / season.episodeCount) else 0
      binding.seasonOverview.setTextWithVisibility(season.generalInfo.overview)
      season.generalInfo.poster?.toImageHolder().loadInto(binding.seasonPoster)
    }

    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return SeasonLarge(
          ItemSeasonLargeBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }

  class Season(val binding: ItemSeasonBinding) : MediaItemViewHolder(binding.root) {
    override val transitionView: View
      get() = binding.root

    override fun bind(item: AVPMediaItem) {
      val season = (item as AVPMediaItem.SeasonItem).season
      binding.seasonTitle.text = season.generalInfo.title
      binding.watchProgress.text = binding.root.context.resources.getString(
        R.string.season_progress_format,
        item.watchedEpisodeNumber ?: 0,
        season.episodeCount
      )
      binding.seasonProgress.progress = if (season.episodeCount > 0)
        ((item.watchedEpisodeNumber ?: 0) * 100 / season.episodeCount) else 0

    }

    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Season(
          ItemSeasonBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }

  class Actor(val binding: ItemActorBinding) : MediaItemViewHolder(binding.root) {
    override val transitionView: View
      get() = binding.background

    override fun bind(item: AVPMediaItem) {
      item as AVPMediaItem.ActorItem
      binding.actorName.setTextWithVisibility(item.actor.name)
      binding.actorRole.setTextWithVisibility(item.actor.role)
      item.poster.loadInto(binding.actorImage, item.placeHolder())
    }


    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Actor(
          ItemActorBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }
  class Track(val binding: ItemTrackBinding) : MediaItemViewHolder(binding.root) {
    private val titleBinding = ItemMediaTitleBinding.bind(binding.root)
    override val transitionView: View
      get() = binding.imageView

    override fun bind(item: AVPMediaItem) {
      item as AVPMediaItem.TrackItem
      titleBinding.title.setTextWithVisibility(item.title)
      titleBinding.subtitle.setTextWithVisibility(item.track.album)
      if(item.poster != null) item.poster.loadInto(binding.imageView)
    }


    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Track(
          ItemTrackBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }

  class Trailer(val binding: ItemVideoBinding) : MediaItemViewHolder(binding.root) {
    override val transitionView: View
      get() = binding.root

    override fun bind(item: AVPMediaItem) {
      item as AVPMediaItem.TrailerItem
      binding.videoTitle.text = item.title
//      binding.voiceActorName.text = item.actorData.voiceActor?.name
//      binding.actorExtra.text = item.actorData.roleString
//      item.poster.loadInto(binding.actorImage, item.placeHolder())
    }

    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Trailer(
          ItemVideoBinding.inflate(layoutInflater, parent, false)
        )
      }
    }

  }


  companion object {
    fun AVPMediaItem.placeHolder() = R.drawable.rounded_rectangle

    fun AVPMediaItem.icon() = when (this) {
      is AVPMediaItem.MovieItem -> R.drawable.ic_video
      is AVPMediaItem.ShowItem -> R.drawable.ic_album
      is AVPMediaItem.SeasonItem -> R.drawable.ic_video
      is AVPMediaItem.EpisodeItem -> R.drawable.ic_video
      is AVPMediaItem.ActorItem -> R.drawable.ic_person
      //is AVPMediaItem.StreamItem -> R.drawable.ic_video
      is AVPMediaItem.TrailerItem -> R.drawable.ic_video
      is AVPMediaItem.PlaybackProgress -> R.drawable.ic_video
      is AVPMediaItem.VideoCollectionItem -> R.drawable.ic_album
      is AVPMediaItem.VideoItem -> R.drawable.ic_video
      is AVPMediaItem.TrackItem -> R.drawable.ic_audio_track
    }
  }
}
