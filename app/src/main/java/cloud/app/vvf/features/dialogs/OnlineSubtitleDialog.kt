package cloud.app.vvf.features.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.subtitle.SubtitleOrigin
import cloud.app.vvf.databinding.DialogOnlineSubtitleBinding
import cloud.app.vvf.network.opensubtitles.OpenSubtitlesClient
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.UIHelper.hideSystemUI
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import cloud.app.vvf.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class OnlineSubtitleDialog(
  private val initialQuery: String?,
  private val onSubtitleSelected: (SubtitleData) -> Unit,
) : DockingDialog() {

  override val widthPercentage = 0.55f

  @Inject lateinit var openSubtitlesClient: OpenSubtitlesClient

  private var binding by autoCleared<DialogOnlineSubtitleBinding>()
  private var results: List<OpenSubtitlesClient.SubtitleResult> = emptyList()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogOnlineSubtitleBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    if (!initialQuery.isNullOrBlank()) {
      binding.mainSearch.setQuery(initialQuery, false)
    }

    binding.mainSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(q: String?): Boolean {
        if (!q.isNullOrBlank()) search(q)
        return true
      }
      override fun onQueryTextChange(newText: String?) = false
    })

    binding.filterLanguage.isVisible = false
    binding.applyBtt.isVisible = false
    binding.cancelBtt.setOnClickListener { dialog?.dismissSafe(activity) }

    binding.listview1.setOnItemClickListener { _, _, position, _ ->
      downloadAndLoad(results[position])
    }

    if (!initialQuery.isNullOrBlank()) search(initialQuery)
  }

  private fun search(query: String) {
    binding.searchLoading.show()
    binding.emptyView.root.isVisible = false
    binding.listview1.isVisible = false

    lifecycleScope.launch {
      results = openSubtitlesClient.search(query)
      binding.searchLoading.hide()

      if (results.isEmpty()) {
        binding.emptyView.root.isVisible = true
        binding.listview1.isVisible = false
      } else {
        val labels = results.map { it.displayLabel }.toTypedArray()
        binding.listview1.adapter = ArrayAdapter(
          requireContext(),
          android.R.layout.simple_list_item_1,
          labels,
        )
        binding.listview1.isVisible = true
        binding.emptyView.root.isVisible = false
      }
    }
  }

  private fun downloadAndLoad(result: OpenSubtitlesClient.SubtitleResult) {
    binding.searchLoading.show()
    binding.listview1.isEnabled = false

    lifecycleScope.launch {
      val url = openSubtitlesClient.getDownloadUrl(result.fileId)
      binding.searchLoading.hide()
      binding.listview1.isEnabled = true

      if (url != null) {
        val subtitleData = SubtitleData(
          name = result.filename,
          url = url,
          origin = SubtitleOrigin.URL,
          mimeType = MimeTypes.APPLICATION_SUBRIP,
          headers = null,
          languageCode = result.language.takeIf { it.isNotEmpty() },
        )
        withContext(Dispatchers.Main) {
          onSubtitleSelected(subtitleData)
          dialog?.dismissSafe(activity)
        }
      } else {
        context?.showToast("Download failed. Set OpenSubtitles API key in settings.")
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    activity?.hideSystemUI()
  }
}
