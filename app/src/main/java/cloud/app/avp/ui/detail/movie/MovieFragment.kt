package cloud.app.avp.ui.detail.movie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cloud.app.avp.databinding.FragmentHomeBinding
import cloud.app.avp.databinding.FragmentMovieBinding
import cloud.app.avp.ui.main.home.HomeViewModel
import cloud.app.avp.ui.media.MediaItemAdapter
import cloud.app.avp.utils.autoCleared
import cloud.app.avp.utils.getParcel
import cloud.app.avp.utils.loadInto
import cloud.app.avp.utils.setupTransition
import cloud.app.common.models.AVPMediaItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MovieFragment : Fragment(), MediaItemAdapter.Listener{
  private var binding by autoCleared<FragmentMovieBinding>()
  private val viewModel by activityViewModels<MovieViewModel>()

  private val args by lazy { requireArguments() }
  private val clientId by lazy { args.getString("clientId")!! }
  private val movieItem by lazy { args.getParcel<AVPMediaItem.MovieItem>("movieItem")!! }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = FragmentMovieBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

//    setupTransition(view)

    movieItem.backdrop.loadInto(binding.headerBackground)
    binding.title.text = movieItem.title
    binding.mediaOverview.text = movieItem.movie.generalInfo.overview

    val actorAdapter = MediaItemAdapter(this, "", "clientID")
    binding.rvActors.adapter = actorAdapter;

  }

  override fun onClick(clientId: String?, item: AVPMediaItem, transitionView: View?) {
    TODO("Not yet implemented")
  }

  override fun onLongClick(clientId: String?, item: AVPMediaItem, transitionView: View?): Boolean {
    TODO("Not yet implemented")
  }


}
