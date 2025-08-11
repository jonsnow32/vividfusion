package cloud.app.vvf.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import cloud.app.vvf.BuildConfig
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.extension.ExtensionRepo.Companion.FEATURE
import cloud.app.vvf.extension.ExtensionRepo.Companion.getPluginFileDir
import cloud.app.vvf.extension.plugger.ApkManifestParser
import cloud.app.vvf.extension.plugger.ApkPluginSource
import cloud.app.vvf.extension.plugger.ApkFileInfo
import cloud.app.vvf.extension.plugger.FileManifestParser
import cloud.app.vvf.ui.extension.ExtensionViewModel
import cloud.app.vvf.utils.registerActivityResultLauncher
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun addApkFile(context: FragmentActivity, file: File): Result<Boolean> = runCatching {
  val viewModel by context.viewModels<ExtensionViewModel>()
  val extensionLoader = viewModel.extensionLoader
  val fileChangeListener = extensionLoader.fileListener
  val packageInfo = context.packageManager.getPackageArchiveInfo(
    file.path, ApkPluginSource.PACKAGE_FLAGS
  )
  val type = getType(packageInfo!!)
  val metadata = ApkManifestParser(ImportType.File)
    .parseManifest(ApkFileInfo(file.path, packageInfo.applicationInfo!!))
  val flow = fileChangeListener.flow
  val dir = context.getPluginFileDir()
  val newFile = File(dir, "${metadata.className}.apk")
  flow.emit(newFile)
  dir.setWritable(true)
  newFile.setWritable(true)
  if (newFile.exists()) newFile.delete()
  file.copyTo(newFile, true)
  dir.setReadOnly()
  flow.emit(null)
  true
}

suspend fun addJarFile(context: FragmentActivity, file: File): Result<Boolean> = runCatching {
  val viewModel by context.viewModels<ExtensionViewModel>()
  val extensionLoader = viewModel.extensionLoader
  val fileChangeListener = extensionLoader.fileListener
  val metadata = FileManifestParser(context.packageManager).parseManifest(file)
  val flow = fileChangeListener.flow
  val dir = context.getPluginFileDir()
  val newFile = File(dir, "${metadata.className}.jar")
  flow.emit(newFile)
  dir.setWritable(true)
  newFile.setWritable(true)
  if (newFile.exists()) newFile.delete()
  file.copyTo(newFile, true)
  dir.setReadOnly()
  flow.emit(null)
  true
}

suspend fun installApk(context: FragmentActivity, file: File): Result<Boolean> = runCatching {
  val contentUri = FileProvider.getUriForFile(
    context, BuildConfig.AUTHORITY_FILE_PROVIDER, file
  )
  val installIntent = Intent(Intent.ACTION_VIEW).apply {
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
    putExtra(Intent.EXTRA_RETURN_RESULT, true)
    data = contentUri
  }
  val it = context.waitForResult(installIntent)
  if (it.resultCode == Activity.RESULT_OK) return@runCatching true
  val result = it.data?.extras?.getInt("android.intent.extra.INSTALL_RESULT")
  throw Exception(installStatusToString(result))
}

suspend fun installExtension(
  context: FragmentActivity, file: File, apk: Boolean
): Result<Boolean> = runCatching {
  return if (apk)
    installApk(context, file)
  else if (file.path.endsWith("apk")) {
    addApkFile(context, file)
  } else
    addJarFile(context, file)
}

suspend fun uninstallExtension(
  context: FragmentActivity, extension: Extension<*>
): Result<Boolean> = runCatching {
  when (extension.metadata.importType) {
    ImportType.BuiltIn -> throw Exception("Built-in extension cannot be uninstalled")
    ImportType.File -> {
      val file = File(extension.metadata.path)
      val viewModel by context.viewModels<ExtensionViewModel>()
      val extensionLoader = viewModel.extensionLoader
      val flow = extensionLoader.fileListener.flow
      flow.emit(file)
      file.parentFile!!.setWritable(true)
      file.setWritable(true)
      val delete = file.delete()
      flow.emit(null)
      delete
    }

    ImportType.App -> {
      val packageName = context.getPackageName(extension.metadata.path)
      val intent = Intent(Intent.ACTION_DELETE).apply {
        data = "package:$packageName".toUri()
        putExtra(Intent.EXTRA_RETURN_RESULT, true)
      }
      context.waitForResult(intent).resultCode == Activity.RESULT_OK
    }
  }
}

fun Context.getPackageName(path: String) = packageManager.getPackageArchiveInfo(
  path, ApkPluginSource.PACKAGE_FLAGS
)!!.packageName

suspend fun FragmentActivity.waitForResult(intent: Intent) = suspendCoroutine { cont ->
  val contract = ActivityResultContracts.StartActivityForResult()
  val activityResultLauncher = registerActivityResultLauncher(contract) {
    cont.resume(it)
  }
  activityResultLauncher.launch(intent)
}

private fun installStatusToString(status: Int?) = when (status) {
  -1 -> "INSTALL_FAILED_ALREADY_EXISTS"
  -2 -> "INSTALL_FAILED_INVALID_APK"
  -3 -> "INSTALL_FAILED_INVALID_URI"
  -4 -> "INSTALL_FAILED_INSUFFICIENT_STORAGE"
  -5 -> "INSTALL_FAILED_DUPLICATE_PACKAGE"
  -6 -> "INSTALL_FAILED_NO_SHARED_USER"
  -7 -> "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
  -8 -> "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE"
  else -> "INSTALL_FAILED : $status"
}


fun getType(appInfo: PackageInfo) = appInfo.reqFeatures?.find { featureInfo ->
  ExtensionType.entries.any { featureInfo.name == "$FEATURE${it.feature}" }
}?.let { featureInfo ->
  ExtensionType.entries.first { it.feature == featureInfo.name.removePrefix(FEATURE) }
} ?: error("Extension type not found for ${appInfo.packageName}")


fun getID(appInfo: PackageInfo) = appInfo.applicationInfo?.metaData?.getString("id")
