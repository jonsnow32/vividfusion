package cloud.app.vvf.ui.update

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cloud.app.vvf.BuildConfig
import cloud.app.vvf.R
import cloud.app.vvf.databinding.FragmentUpdateBinding
import cloud.app.vvf.services.downloader.ApkDownloader
import cloud.app.vvf.utils.AppUpdater
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.putSerialized
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class UpdateFragment : Fragment() {

  @Inject
  lateinit var appUpdater: AppUpdater

  @Inject
  lateinit var prefs: SharedPreferences

  private var binding by autoCleared<FragmentUpdateBinding>()
  private var expectedVersion: String? = null
  private var currentWorkId: String? = null

  val release by lazy {
    arguments?.getSerialized<AppUpdater.GitHubRelease>("release")
      ?: throw IllegalArgumentException("GitHubRelease is required")
  }

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    Timber.d("Notification permission ${if (isGranted) "granted" else "denied"}")
    startUpdateProcess()
  }

  companion object {
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
      return context?.packageManager?.getPackageInfo(context.packageName, 0)?.versionName?.let { "v$it" }
        ?: "Unknown"
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentUpdateBinding.inflate(inflater, container, false)

    val currentVersion = getCurrentVersion(context)
    val newVersion = release.tag_name
    expectedVersion = newVersion.trimStart('v')

    binding.versionInfoText.text = getString(R.string.update_title, currentVersion, newVersion)
    binding.statusText.text = getString(R.string.checking_for_updates)
    binding.installBtn.visibility = View.GONE

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      startUpdateProcess()
    }

    return binding.root
  }

  override fun onStart() {
    super.onStart()
    observeWorkerStatus()
  }

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch(Dispatchers.Main) {
      val currentVersion = getCurrentVersion(context)
      if (prefs.getBoolean(KEY_IS_UPDATING, false) && currentVersion != expectedVersion) {
        Timber.w("Installation cancelled: Current=$currentVersion, Expected=$expectedVersion")
        binding.statusText.text = getString(R.string.installation_cancelled)
        prefs.edit().putBoolean(KEY_IS_UPDATING, false).apply()

      }
    }
  }

  private fun startUpdateProcess() {
    lifecycleScope.launch {
      try {
        val tag = AppUpdater.APK_DOWNLOAD_WORK_TAG
        val workManager = WorkManager.getInstance(requireContext())
        val workInfos = workManager.getWorkInfosByTag(tag).get()

        val finishedWorker = workInfos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED && it.outputData.getString(ApkDownloader.FileParams.KEY_TAG_LAST_UPDATE) == release.published_at }
        if (finishedWorker != null) {
          val fileUriString = finishedWorker.outputData.getString(ApkDownloader.FileParams.KEY_FILE_URI)
          if (fileUriString != null) {
            currentWorkId = finishedWorker.id.toString()
            Timber.d("Found completed worker: $currentWorkId")
            updateUIForSuccess(fileUriString)
            return@launch
          }
        }

        val activeWorker = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        if (activeWorker != null) {
          currentWorkId = activeWorker.id.toString()
          Timber.d("Using active worker: $currentWorkId")
        } else {
          val workRequestId = appUpdater.enqueueDownload(release)
          if (workRequestId.isNotEmpty()) {
            currentWorkId = workRequestId
            Timber.d("Enqueued new worker: $workRequestId")
          } else {
            Timber.d("Using cached APK")
            val fileName = release.assets.find { it.name.endsWith(".apk") }?.name?.removeSuffix(".apk")
            val cachedFile = File(context?.cacheDir, "$fileName.apk")
            if (cachedFile.exists()) {
              installApk(Uri.fromFile(cachedFile))
              binding.statusText.text = getString(R.string.installing)
            } else {
              handleFailure("Cached file missing")
            }
            return@launch
          }
        }

        observeWorkerStatus()
      } catch (e: Exception) {
        Timber.e(e, "Update process failed")
        handleFailure(e.message ?: "Unknown error")
      }
    }
  }

  private fun observeWorkerStatus() {
    val tag = AppUpdater.APK_DOWNLOAD_WORK_TAG
    WorkManager.getInstance(requireContext())
      .getWorkInfosByTagLiveData(tag)
      .observe(viewLifecycleOwner) { workInfos ->
        val workInfo = workInfos.firstOrNull { it.id.toString() == currentWorkId }
        when (workInfo?.state) {
          WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
            Timber.d("Worker ${workInfo.state.name}: ${workInfo.id}")
            binding.statusText.text = getString(R.string.downloading_update)
            binding.progressBar.progress = 0
            binding.percentageText.text = "0%"
            binding.installBtn.visibility = View.GONE
          }
          WorkInfo.State.RUNNING -> {
            val progress = workInfo.progress.getInt("progress", 0)
            Timber.d("Worker running: ${workInfo.id}, $progress%")
            binding.statusText.text = getString(R.string.downloading_update)
            binding.progressBar.progress = progress
            binding.percentageText.text = "$progress%"
            binding.installBtn.visibility = View.GONE
          }
          WorkInfo.State.SUCCEEDED -> {
            val fileUriString = workInfo.outputData.getString(ApkDownloader.FileParams.KEY_FILE_URI)
            if (fileUriString != null) {
              Timber.i("Worker succeeded: ${workInfo.id}")
              updateUIForSuccess(fileUriString)
            } else {
              Timber.e("No URI from worker: ${workInfo.id}")
              handleFailure(getString(R.string.no_file_uri_returned))
            }
          }
          WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
            Timber.w("Worker ${workInfo.state.name}: ${workInfo.id}")
            handleFailure(getString(if (workInfo.state == WorkInfo.State.FAILED) R.string.download_failed else R.string.download_cancelled))
          }
          null -> {
            Timber.d("No worker found for ID: $currentWorkId")
            binding.statusText.text = getString(R.string.checking_for_updates)
            binding.progressBar.progress = 0
            binding.percentageText.text = "0%"
            binding.installBtn.visibility = View.GONE
          }
        }
      }
  }

  private fun updateUIForSuccess(fileUriString: String) {
    binding.statusText.text = getString(R.string.downloaded_update)
    binding.progressBar.progress = 100
    binding.percentageText.text = "100%"
    binding.installBtn.visibility = View.VISIBLE
    binding.installBtn.setOnClickListener {
      binding.statusText.text = getString(R.string.installing)
      prefs.edit()
        .putString(KEY_UPDATING_VERSION, expectedVersion)
        .putBoolean(KEY_IS_UPDATING, true)
        .apply()
      installApk(Uri.parse(fileUriString))
    }
  }

  private fun installApk(apkUri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(
        FileProvider.getUriForFile(
          requireContext(),
          BuildConfig.AUTHORITY_FILE_PROVIDER,
          File(apkUri.path ?: throw IllegalArgumentException("Invalid APK URI"))
        ),
        "application/vnd.android.package-archive"
      )
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(intent)
  }

  private fun handleFailure(message: String) {
    binding.statusText.text = getString(R.string.update_fail, message)
    binding.progressBar.progress = 0
    binding.percentageText.text = "0%"
    binding.installBtn.visibility = View.GONE
    prefs.edit().putBoolean(KEY_IS_UPDATING, false).apply()
  }
}
