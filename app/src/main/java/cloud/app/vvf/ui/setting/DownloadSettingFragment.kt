package cloud.app.vvf.ui.setting

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import cloud.app.vvf.R
import cloud.app.vvf.services.downloader.helper.DownloadFileManager
import cloud.app.vvf.utils.FileFolderPicker.getChooseFolderLauncher
import cloud.app.vvf.utils.FileHelper
import cloud.app.vvf.utils.MaterialListPreference
import cloud.app.vvf.utils.MaterialSliderPreference
import cloud.app.vvf.utils.TV
import cloud.app.vvf.utils.isLayout
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

class DownloadSettingFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.download)
  override val transitionName = "download_settings"
  override val container = { UiPreference() }

  @AndroidEntryPoint
  class UiPreference : PreferenceFragmentCompat() {

    lateinit var downloadFileManager: DownloadFileManager

    @Inject
    lateinit var fileHelper: FileHelper

    private val pathPicker = getChooseFolderLauncher { uri ->
      val context = context ?: return@getChooseFolderLauncher
      val uri = uri ?: return@getChooseFolderLauncher
      updateDownloadPathPreference(uri)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context

      downloadFileManager = DownloadFileManager(context)

      preferenceManager.sharedPreferencesName = context.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(context)
      val preferences = preferenceManager.sharedPreferences ?: return
      preferenceScreen = screen
      fun uiListener(block: (Any) -> Unit = {}) =
        Preference.OnPreferenceChangeListener { pref, new ->
          block(new)
          true
        }

      // Backup Path Preference
      FolderMaterialListPreference(context) {
        try {
          pathPicker.launch(null)
        } catch (e: Exception) {
          Timber.e(e, "Failed to launch path picker")
        }
      }.apply {
        val downloadDirs =  fileHelper.getAllowedPaths(downloadFileManager.getDownloadUri() )
        layoutResource = R.layout.preference
        key = getString(R.string.pref_download_path)
        title = getString(R.string.download_path)
        icon = AppCompatResources.getDrawable(context, R.drawable.folder_24dp)
        entries = downloadDirs.map { file -> file.filePath ?: "" }.toTypedArray()
        entryValues = downloadDirs.map { file -> file.uri.toString() }.toTypedArray()
        val defaultValue = downloadDirs.firstOrNull()?.uri.toString()
        value = preferences.getString(key, defaultValue) ?: defaultValue
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          true // Allow the custom dialog to handle clicks
        }
        preferenceScreen.addPreference(this)
      }

      MaterialSliderPreference(context, 1, 10, 1, allowOverride = false).apply {
        key = getString(R.string.pref_download_batch_size)
        title = getString(R.string.download_batch_size)
        summary = getString(R.string.download_batch_size_sumary)
        icon = AppCompatResources.getDrawable(context, R.drawable.outline_segment_24)
        suffixSummary = ""
        isIconSpaceReserved = false
        setDefaultValue(Runtime.getRuntime().availableProcessors().coerceAtLeast(1))
        screen.addPreference(this)
      }

//      PreferenceCategory(context).apply {
//        title = getString(R.string.hls_options)
//        key = "hls_option"
//        isIconSpaceReserved = false
//        layoutResource = R.layout.preference_category
//        screen.addPreference(this)
//
//        MaterialSliderPreference(context, 1, 10, 1, allowOverride = false).apply {
//          key = getString(R.string.pref_download_batch_size)
//          title = getString(R.string.download_batch_size)
//          summary = getString(R.string.download_batch_size_sumary)
//          suffixSummary = ""
//          isIconSpaceReserved = false
//          setDefaultValue(1)
//          addPreference(this)
//        }
//      }
    }

    private fun updateDownloadPathPreference(uri: Uri) {
      downloadFileManager.saveDownloadDirectoryPreference(uri)
      fileHelper.addAllowedPath(uri)
      val pref = findPreference<FolderMaterialListPreference>(getString(R.string.pref_download_path))
      pref?.let {
        val backupDirs = fileHelper.getAllowedPaths(downloadFileManager.getDownloadUri())
        it.entries = backupDirs.map { file -> file.filePath ?: "" }.toTypedArray()
        it.entryValues = backupDirs.map { file -> file.uri.toString() }.toTypedArray()
        it.value = uri.toString()
      }
    }
  }


}
