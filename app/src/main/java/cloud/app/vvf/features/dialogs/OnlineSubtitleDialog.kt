package cloud.app.vvf.features.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.databinding.DialogOnlineSubtitleBinding
import cloud.app.vvf.ui.setting.appLanguages
import cloud.app.vvf.ui.setting.getCurrentLocale
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.SubtitleHelper
import cloud.app.vvf.utils.Utils.hideKeyboard
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max

@AndroidEntryPoint
@UnstableApi
class OnlineSubtitleDialog : DockingDialog() {
  override val widthPercentage = 0.7f
  private val viewModel by viewModels<SubtitleViewModel>()
  private var binding by autoCleared<DialogOnlineSubtitleBinding>()
  private val query by lazy { arguments?.getString("query") }
  private val langCodes by lazy { arguments?.getStringArray("langCodes") }
  private val avpMediaItem by lazy { arguments?.getSerialized<AVPMediaItem>("avpMediaItem") }
  private val addedSubtitles by lazy { arguments?.getSerialized<List<String>>("addedSubtitles") }

  private var selectedLanguages: MutableList<String>? = null

  private val adapter by lazy {
    ArrayAdapter(
      requireContext(),
      R.layout.sort_bottom_single_choice,
      mutableListOf<SubtitleData>()
    )
  }

  companion object {
    fun newInstance(
      query: String?,
      avpMediaItem: AVPMediaItem?,
      addedSubtitles: List<String>?,
      langCodes: Array<String>?
    ) =
      OnlineSubtitleDialog().apply {
        arguments = Bundle().apply {
          putSerialized("avpMediaItem", avpMediaItem)
          putSerialized("addedSubtitles", addedSubtitles)
          putStringArray("langCodes", langCodes)
          putString("query", query)
        }
      }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogOnlineSubtitleBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val context = this.context ?: return
    setupDialog()
    query?.let {
      viewModel.findSubtitle(
        SearchItem(
          it,
          extras = getExtras(),
          searchedAt = System.currentTimeMillis()
        )
      )
    }
  }

  fun getExtras() = mutableMapOf<String, String?>().apply {
    val ids = when (avpMediaItem) {
      is AVPMediaItem.MovieItem -> (avpMediaItem as AVPMediaItem.MovieItem).movie.ids
      is AVPMediaItem.ShowItem -> (avpMediaItem as AVPMediaItem.ShowItem).show.ids
      else -> null
    }
    val episodeNumber = avpMediaItem as? AVPMediaItem.EpisodeItem
    set("imdbId", ids?.imdbId)
    set("tmdbId", ids?.tmdbId?.toString())
    set("year", avpMediaItem?.releaseYear.toString())
    set("epNumber", episodeNumber?.episode?.episodeNumber.toString())
    set("seasonNumber", episodeNumber?.episode?.seasonNumber.toString())
    set("lang", (selectedLanguages ?: langCodes?.toList())?.joinToString(","))
  }

  @SuppressLint("RestrictedApi")
  private fun setupDialog() {
    binding.apply {
      listview1.adapter = adapter
      listview1.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
      listview1.setOnItemClickListener { _, _, position, _ ->
        adapter.getItem(position)?.let { addSubtitle(it) }
      }
      mainSearch.setQuery(query, false)
      when (avpMediaItem) {
        is AVPMediaItem.MovieItem,
        is AVPMediaItem.ShowItem,
        is AVPMediaItem.VideoItem,
        is AVPMediaItem.EpisodeItem -> {
          mainSearch.queryHint = getString(R.string.search_subtitle_hint)
        }

        is AVPMediaItem.TrackItem ->
          mainSearch.queryHint = getString(R.string.search_lyric_hint)

        else -> {}
      }

      mainSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
          query?.let { searchQuery ->
            val searchItem = SearchItem(searchQuery, searchedAt = System.currentTimeMillis())
            searchItem.extras = getExtras()

            val imdbIdRegex = Regex("tt\\d{7,8}\\b")
            val yearRegex = Regex("\\b(18[8-9]\\d|19\\d{2}|20\\d{2}|209\\d)\\b")
            val year = yearRegex.find(searchQuery)?.value
            val imdbId = imdbIdRegex.find(searchQuery)?.value


            val titleRegex = Regex("^(.+?)(?=\\s*[sS]\\d+[eE]\\d+)", RegexOption.IGNORE_CASE)
            val seasonRegex = Regex("[sS](\\d{1,2})", RegexOption.IGNORE_CASE)
            val episodeRegex = Regex("[eE](\\d{1,2})", RegexOption.IGNORE_CASE)

            val title = titleRegex.find(searchQuery)?.groups?.get(1)?.value?.trim()
            val season = seasonRegex.find(searchQuery)?.groups?.get(1)?.value
            val episode = episodeRegex.find(searchQuery)?.groups?.get(1)?.value

            searchItem.extras?.apply {
              set("year", year)
              set("imdbId", imdbId)
              set("title", title)
              set("epNumber",episode)
              set("seasonNumber", season)
            }
            viewModel.findSubtitle(searchItem)
          }
          hideKeyboard(mainSearch)
          return true // Indicate the event was handled
        }

        override fun onQueryTextChange(newText: String?): Boolean {
          return false // Not handling text changes for now
        }
      })

      val searchAutoComplete = mainSearch.findViewById<SearchAutoComplete>(
        androidx.appcompat.R.id.search_src_text
      )
      // Set paddingRight (e.g., 16dp)
      val paddingRightPx = (32 * resources.displayMetrics.density).toInt() // Convert 16dp to pixels
      searchAutoComplete.setPadding(
        searchAutoComplete.paddingLeft, // Keep existing left padding
        searchAutoComplete.paddingTop,  // Keep existing top padding
        paddingRightPx,                // Set right padding
        searchAutoComplete.paddingBottom // Keep existing bottom padding
      )

      applyBtt.setOnClickListener {
        dismissSafe()
      }
      cancelBtt.setOnClickListener {
        selectedItems.clear()
        dismissSafe()
      }
      filterLanguage.setOnClickListener {
        val tempLang = appLanguages.toMutableList()
        val current = getCurrentLocale(requireContext())
        val languageCodes = tempLang.map { (_, _, iso) -> iso }
        val languageNames = tempLang.map { (emoji, name, iso) ->
          val flag = emoji.ifBlank { SubtitleHelper.getFlagFromIso(iso) ?: "ERROR" }
          "$flag $name"
        }
        val index = max(languageCodes.indexOf(current), 0)
        SelectionDialog.multiple(languageNames, listOf(index), "Select Language")
          .show(parentFragmentManager) { result ->
            result?.let {
              it.getIntegerArrayList("selected_items")?.let {
                selectedLanguages =
                  languageCodes.filterIndexed { index, s -> it.contains(index) }
                    .toMutableList()

                query?.let { q ->
                  viewModel.findSubtitle(
                    SearchItem(
                      q,
                      extras = getExtras(),
                      searchedAt = System.currentTimeMillis()
                    )
                  )
                }
              }
            }
          }
      }
    }

    observe(viewModel.subtitles) { subtitles ->
      if (subtitles?.isEmpty() == true) {
        binding.emptyView.root.isGone = false
      } else {
        if (subtitles != null) {
          binding.emptyView.root.isGone = true
          val newSubtitles = subtitles.filterNot { subtitleData ->
            addedSubtitles?.any { url -> url.contains(subtitleData.url) } == true
          }
          adapter.clear()
          adapter.addAll(newSubtitles)
          adapter.notifyDataSetChanged()
        }
      }
    }
    observe(viewModel.loading) {
      binding.searchLoading.isVisible = it
      val closeButton =
        binding.mainSearch.findViewById<View>(androidx.appcompat.R.id.search_close_btn)
      closeButton?.isVisible = !it
    }
  }

  private val selectedItems = mutableListOf<SubtitleData>()

  private fun addSubtitle(subtitle: SubtitleData) {
    if (!selectedItems.contains(subtitle)) {
      selectedItems.add(subtitle)
    }
  }

  override fun getResultBundle(): Bundle? {
    return if (selectedItems.isNotEmpty()) {
      Bundle().apply {
        putSerialized("selected_items", selectedItems)
      }
    } else {
      null
    }
  }

  private fun dismissSafe() {
    dialog?.dismissSafe(activity)
  }
}
