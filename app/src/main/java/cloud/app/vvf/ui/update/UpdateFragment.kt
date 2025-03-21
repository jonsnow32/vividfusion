package cloud.app.vvf.ui.update

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cloud.app.vvf.MainActivityViewModel.Companion.applyInsets
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentUpdateBinding
import cloud.app.vvf.utils.AppUpdater
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.putSerialized
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UpdateFragment : Fragment() {

  @Inject
  lateinit var appUpdater: AppUpdater
  @Inject
  lateinit var prefs: SharedPreferences

  private var binding by autoCleared<FragmentUpdateBinding>()
  private var expectedVersion: String? = null

  companion object {
    private const val PREFS_NAME = "UpdatePrefs"
    private const val KEY_UPDATING_VERSION = "updatingVersion"
    private const val KEY_IS_UPDATING = "isUpdating"

    fun newInstance(release: AppUpdater.GitHubRelease): UpdateFragment {
      return UpdateFragment().apply {
        arguments = Bundle().apply {
          putSerialized("release", release)
        }
      }
    }

    private fun getCurrentVersion(context: Context?): String {
      val context = context ?: return "Unknown"
      return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
      } catch (e: Exception) {
        Timber.e(e, "Failed to get current version")
        "Unknown"
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentUpdateBinding.inflate(inflater, container, false)

    val currentVersion = getCurrentVersion(context)
    val release = arguments?.getSerialized<AppUpdater.GitHubRelease>("release")
      ?: throw IllegalArgumentException("GitHubRelease is required")
    val newVersion = release.tag_name
    expectedVersion = newVersion.trimStart('v')

    binding.versionInfoText.text = getString(R.string.update_title, currentVersion, newVersion)
    binding.statusText.text = getString(R.string.downloading_update)
    startUpdateProcess(release)

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    Timber.d("onResume")
    lifecycleScope.launch(Dispatchers.Main) {
      val currentVersion = getCurrentVersion(context)
      val isUpdating = prefs.getBoolean(KEY_IS_UPDATING, false)
      if (isUpdating && currentVersion != expectedVersion) {
        Timber.w("Installation cancelled: Current=$currentVersion, Expected=$expectedVersion")
        binding.statusText.text = getString(R.string.installation_cancelled)
        prefs.edit().putBoolean(KEY_IS_UPDATING, false).apply()
      }
    }
  }

  private fun startUpdateProcess(release: AppUpdater.GitHubRelease) {
    lifecycleScope.launch {
      try {
        appUpdater.downloadAndInstall(
          release,
          onProgress = { downloaded, total ->
            val progress = if (total > 0) (downloaded * 100 / total).toInt() else 0
            launch(Dispatchers.Main) {
              Timber.d("UI Update: Progress=$progress% ($downloaded/$total)")
              binding.progressBar.progress = progress
              binding.percentageText.text = "$progress%"
            }
          }
        )

        withContext(Dispatchers.Main) {
          binding.statusText.text = getString(R.string.installing)
          prefs.edit()
            .putString(KEY_UPDATING_VERSION, release.tag_name.trimStart('v'))
            .putBoolean(KEY_IS_UPDATING, true)
            .apply()
        }
      } catch (e: Exception) {
        Timber.e(e, "Update failed")
        withContext(Dispatchers.Main) {
          binding.statusText.text = getString(R.string.update_fail, e.message)
          binding.progressBar.progress = 0
          binding.percentageText.text = "0%"
          prefs.edit().putBoolean(KEY_IS_UPDATING, false).apply()
        }
      }
    }
  }
}
