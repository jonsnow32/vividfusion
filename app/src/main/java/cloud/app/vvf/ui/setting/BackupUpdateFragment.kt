package cloud.app.vvf.ui.setting

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.R
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.datastore.account.AccountDataStore
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.services.BackupWorker
import cloud.app.vvf.ui.update.UpdateFragment
import cloud.app.vvf.ui.widget.dialog.SelectionDialog
import cloud.app.vvf.utils.AppUpdater
import cloud.app.vvf.utils.BackupHelper
import cloud.app.vvf.utils.FileFolderPicker.getChooseFileLauncher
import cloud.app.vvf.utils.FileFolderPicker.getChooseFolderLauncher
import cloud.app.vvf.utils.KUniFile
import cloud.app.vvf.utils.Utils
import cloud.app.vvf.utils.navigate
import cloud.app.vvf.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

class BackupUpdateFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.backup_updates)
  override val transitionName = "backup_restore"
  override val container = { BackupRestorePreference() }

  @AndroidEntryPoint
  class BackupRestorePreference : PreferenceFragmentCompat() {
    @Inject
    lateinit var appDataStore: MutableStateFlow<AppDataStore>

    @Inject
    lateinit var accountDataStore: MutableStateFlow<AccountDataStore>

    @Inject
    lateinit var extensionsFlow: MutableStateFlow<List<Extension<*>>>

    @Inject
    lateinit var backupHelper: BackupHelper

    @Inject
    lateinit var throwableFlow: MutableSharedFlow<Throwable>

    @Inject
    lateinit var appUpdater: AppUpdater

    private val pathPicker = getChooseFolderLauncher { uri ->
      val context = context ?: return@getChooseFolderLauncher
      val uri = uri ?: return@getChooseFolderLauncher
      updateBackupPathPreference(uri)
    }

    private val restorePicker = getChooseFileLauncher { uri ->
      uri?.let {
        lifecycleScope.launch(Dispatchers.IO) {
          val result = backupHelper.restoreSharedPreferencesFromJson(it)
          withContext(Dispatchers.Main) {
            if (result) {
              context?.showToast(R.string.restore_success)
            } else {
              context?.showToast(R.string.restore_fail)

            }
          }
        }
      }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = context.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val preferences = preferenceManager.sharedPreferences ?: return

      val screen = preferenceManager.createPreferenceScreen(context)
      preferenceScreen = screen

      val backupCategoryPreference = PreferenceCategory(context).apply {
        title = getString(R.string.backup)
        layoutResource = R.layout.preference_category
        screen.addPreference(this)
      }


      Preference(context).apply {
        title = getString(R.string.backup_now)
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(context, R.drawable.download_2_24dp)
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          lifecycleScope.launch(Dispatchers.IO) {
            // Define backup file using KUniFile
            val defaultDir = backupHelper.getDefaultBackupDir()
            val date = SimpleDateFormat("yyyy_MM_dd_HH_mm").format(Date(System.currentTimeMillis()))
            val displayName = "vvf_backup${date}.json"
            val backupDirPath = preferences.getString(
              getString(R.string.pref_backup_path),
              defaultDir?.uri?.toString()
            )
            val backupDir =
              backupDirPath?.let { KUniFile.fromUri(context, Uri.parse(it)) } ?: defaultDir
            if (backupDir?.isDirectory == true) {
              val backupFile = backupDir.createFile(displayName, "application/json")

              if (backupFile != null) {
                // Backup all SharedPreferences
                val allPrefs = backupHelper.getAllSharedPrefsNames()
                val backupAllSuccess =
                  backupHelper.backupSharedPreferencesToJson(allPrefs, backupFile)

                withContext(Dispatchers.Main) {
                  if (backupAllSuccess) {
                    context.showToast(R.string.backup_success)
                    summary = backupFile.filePath
                  } else {
                    context.showToast(R.string.backup_fail)
                  }
                }
              } else {
                withContext(Dispatchers.Main) {
                  context.showToast(R.string.backup_fail)
                }
              }
            }

          }
          true
        }
        backupCategoryPreference.addPreference(this)
      }

      Preference(context).apply {
        title = getString(R.string.restore)
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(context, R.drawable.sync_24dp)
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          restorePicker.launch(
            arrayOf(
              "text/plain",
              "text/str",
              "text/x-unknown",
              "application/json",
              "unknown/unknown",
              "content/unknown",
              "application/octet-stream"
            )
          )
          true
        }
        backupCategoryPreference.addPreference(this)
      }

      // Backup Frequency Preference
      Preference(context).apply {
        val prefNames = resources.getStringArray(R.array.periodic_work_names)
        val prefValues = resources.getIntArray(R.array.periodic_work_values)
        val current = preferences.getInt(getString(R.string.pref_automatic_backup_key), 0)
        summary = prefNames[prefValues.indexOf(current)]

        title = getString(R.string.backup_frequency)
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(context, R.drawable.schedule_24dp)
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          SelectionDialog.single(
            prefNames.toList(),
            prefValues.indexOf(current),
            getString(R.string.backup_frequency),
            true
          ).show(parentFragmentManager) { result ->
            val selected = result?.getIntegerArrayList("selected_items")?.get(0) ?: 0
            preferences.edit()
              .putInt(getString(R.string.pref_automatic_backup_key), prefValues[selected])
              .apply()
            summary = prefNames[selected]
            BackupWorker.enqueuePeriodicWork(context, prefValues[selected].toLong())
          }
          true
        }
        backupCategoryPreference.addPreference(this)
      }

      // Backup Path Preference
      BackupMaterialListPreference(context) {
        try {
          pathPicker.launch(null)
        } catch (e: Exception) {
          Timber.e(e, "Failed to launch path picker")
        }
      }.apply {
        val backupDirs = getBackupDirsForDisplay()
        layoutResource = R.layout.preference
        key = getString(R.string.pref_backup_path)
        title = getString(R.string.backup_path)
        icon = AppCompatResources.getDrawable(context, R.drawable.folder_24dp)
        entries = backupDirs.map { file -> file.filePath ?: "" }.toTypedArray()
        entryValues = backupDirs.map { file -> file.uri.toString() }.toTypedArray()
        val defaultValue = backupDirs.firstOrNull()?.uri.toString()
        value = preferences.getString(key, defaultValue) ?: defaultValue
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          true // Allow the custom dialog to handle clicks
        }
        backupCategoryPreference.addPreference(this)
      }

      val updateCategoryPreference = PreferenceCategory(context).apply {
        title = getString(R.string.update)
        layoutResource = R.layout.preference_category
        screen.addPreference(this)
      }

      Preference(context).apply {
        title = getString(R.string.check_for_update)
        layoutResource = R.layout.preference
        icon = AppCompatResources.getDrawable(context, R.drawable.download_2_24dp)
        summary = Utils.getBuildVersionName(context)
        onPreferenceClickListener = Preference.OnPreferenceClickListener {
          lifecycleScope.launch {
            val context = context
            try {
              val latestRelease = appUpdater.checkForUpdate()
              if (latestRelease != null) {
                parentFragment?.navigate(
                  UpdateFragment.newInstance(latestRelease), null, true
                ) ?: context.showToast(R.string.update_fail)
              } else {
                context.showToast(R.string.no_update_availiable)

              }
            } catch (e: Exception) {
              context.showToast(getString(R.string.update_fail, e.message))
            }
          }
          true
        }
        updateCategoryPreference.addPreference(this)
      }

      SwitchPreferenceCompat(context).apply {
        title = getString(R.string.show_check_update)
        layoutResource = R.layout.preference_switch
        icon = AppCompatResources.getDrawable(context, R.drawable.notify_24dp)
        key = getString(R.string.pref_auto_check_update)
        summary = getString(R.string.show_check_update_summary)
        updateCategoryPreference.addPreference(this)
      }
    }

    private fun updateBackupPathPreference(uri: Uri) {
      backupHelper.storeBackupPath(uri)
      val pref = findPreference<BackupMaterialListPreference>(getString(R.string.pref_backup_path))
      pref?.let {
        val backupDirs = getBackupDirsForDisplay()
        it.entries = backupDirs.map { file -> file.filePath ?: "" }.toTypedArray()
        it.entryValues = backupDirs.map { file -> file.uri.toString() }.toTypedArray()
        it.value = uri.toString()
      }
    }

    private fun getBackupDirsForDisplay(): List<KUniFile> {
      val context = context ?: return emptyList()
      val defaultDir = backupHelper.getDefaultBackupDir()
      val path = backupHelper.getAllowedBackupPaths();
      val allowedPath = if (!path.isNullOrEmpty()) KUniFile.fromUri(
        context,
        Uri.parse(backupHelper.getAllowedBackupPaths())
      ) else null
      return listOfNotNull(defaultDir, allowedPath)
    }
  }
}
