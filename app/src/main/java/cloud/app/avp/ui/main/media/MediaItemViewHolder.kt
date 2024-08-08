package cloud.app.avp.ui.main.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.R
import cloud.app.avp.databinding.ItemActorBinding
import cloud.app.avp.databinding.ItemMediaTitleBinding
import cloud.app.avp.databinding.ItemMovieBinding
import cloud.app.avp.databinding.ItemMovieCoverBinding
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.roundTo
import cloud.app.common.models.AVPMediaItem

sealed class MediaItemViewHolder(itemView: View) :
  RecyclerView.ViewHolder(itemView) {
  abstract fun bind(item: AVPMediaItem)
  abstract val transitionView: View


  fun ItemMediaTitleBinding.bind(item: AVPMediaItem) {
    title.text = item.title
    subtitle.isVisible = item.subtitle.isNullOrEmpty().not()
    subtitle.text = item.subtitle
  }

  fun ItemMovieCoverBinding.bind(item: AVPMediaItem) {
    item.poster.loadInto(movieImageView, item.placeHolder())
    this.iconContainer.isVisible = item.rating != null
    this.rating.text = item.rating?.roundTo(1).toString()
  }

  class Movie(val binding: ItemMovieBinding) : MediaItemViewHolder(binding.root) {
    private val titleBinding = ItemMediaTitleBinding.bind(binding.root)
    override val transitionView: View
      get() = binding.cover.root

    override fun bind(item: AVPMediaItem) {
      titleBinding.bind(item)
      binding.cover.bind(item)
    }

    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Movie(
          ItemMovieBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }


  class Episode(val binding: ItemMovieBinding) : MediaItemViewHolder(binding.root) {
    private val titleBinding = ItemMediaTitleBinding.bind(binding.root)
    override val transitionView: View
      get() = binding.cover.root

    override fun bind(item: AVPMediaItem) {
      titleBinding.bind(item)
      binding.cover.bind(item)
    }

    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Episode(
          ItemMovieBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }

  class Show(val binding: ItemMovieBinding) : MediaItemViewHolder(binding.root) {
    private val titleBinding = ItemMediaTitleBinding.bind(binding.root)
    override val transitionView: View
      get() = binding.cover.root

    override fun bind(item: AVPMediaItem) {
      titleBinding.bind(item)
      binding.cover.bind(item)
    }

    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Show(
          ItemMovieBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }

  class Actor(val binding: ItemActorBinding) : MediaItemViewHolder(binding.root) {
    override val transitionView: View
      get() = binding.background

    override fun bind(item: AVPMediaItem) {
      item as AVPMediaItem.ActorItem
      binding.actorName.text = item.actorData.actor.name
      binding.voiceActorName.text = item.actorData.voiceActor?.name
      binding.actorExtra.text = item.actorData.roleString
      item.poster.loadInto(binding.actorImage, item.placeHolder())
    }


    companion object {
      fun create(
        parent: ViewGroup
      ): MediaItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return Show(
          ItemMovieBinding.inflate(layoutInflater, parent, false)
        )
      }
    }
  }


  companion object {
    fun AVPMediaItem.placeHolder() = when (this) {
      is AVPMediaItem.MovieItem -> R.drawable.rounded_rectangle
      is AVPMediaItem.ShowItem -> R.drawable.rounded_rectangle
      is AVPMediaItem.EpisodeItem -> R.drawable.art_user
      is AVPMediaItem.ActorItem -> R.drawable.art_album
    }

    fun AVPMediaItem.icon() = when (this) {
      is AVPMediaItem.MovieItem -> R.drawable.ic_video
      is AVPMediaItem.ShowItem -> R.drawable.ic_album
      is AVPMediaItem.EpisodeItem -> R.drawable.ic_person
      is AVPMediaItem.ActorItem -> R.drawable.ic_album
    }

  }
}
