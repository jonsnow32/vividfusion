package cloud.app.vvf.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import cloud.app.vvf.R
import java.util.UUID


fun ComponentActivity.requestVideoPermission() {
  val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    Manifest.permission.READ_MEDIA_VIDEO
  else
    Manifest.permission.READ_EXTERNAL_STORAGE
  requestPermissions(
    this,
    perm,
    R.string.permission_required,
    R.string.music_permission_required_summary,
    { finish() }
  )?.launch(perm)
}


fun ComponentActivity.requestAudioPermissions() {
  val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    Manifest.permission.READ_MEDIA_AUDIO
  else
    Manifest.permission.READ_EXTERNAL_STORAGE
  requestPermissions(
    this,
    perm,
    R.string.permission_required,
    R.string.music_permission_required_summary,
    { finish() }
  )?.launch(perm)
}

fun ComponentActivity.requestPermission(permission: String, onCancel: () -> Unit, onGranted: () -> Unit) {
  requestPermissions(
    this,
    permission,
    R.string.permission_required,
    R.string.music_permission_required_summary,
    onCancel,onGranted
  )?.launch(permission)
}


fun ComponentActivity.checkPermission(
  context: Context,
  perm: String,
): Boolean = with(context) {
  return  ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
}


fun ComponentActivity.requestPermissions(
  context: Context,
  perm: String,
  title: Int,
  message: Int,
  onCancel: () -> Unit,
  onGranted: () -> Unit = {},
  onRequest: () -> Unit = {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      context.startActivity(this)
    }
  }
): ActivityResultLauncher<String>? = with(context) {
  val permStatus = ContextCompat.checkSelfPermission(this, perm)
  val contract = ActivityResultContracts.RequestPermission()
  return if (permStatus != PackageManager.PERMISSION_GRANTED)
    registerActivityResultLauncher(contract) {
      if (!it) AlertDialog.Builder(this)
        .setTitle(getString(title))
        .setMessage(getString(message))
        .setPositiveButton(getString(R.string.ok)) { _, _ -> onRequest() }
        .setNegativeButton(getString(R.string.cancel)) { _, _ ->
          showToast(getString(R.string.permission_denied))

          onCancel()
        }
        .show()
      else onGranted()
    } else null
}

fun <I, O> ComponentActivity.registerActivityResultLauncher(
  contract: ActivityResultContract<I, O>,
  block: (O) -> Unit
): ActivityResultLauncher<I> {
  val key = UUID.randomUUID().toString()
  var launcher: ActivityResultLauncher<I>? = null
  val callback = ActivityResultCallback<O> {
    block.invoke(it)
    launcher?.unregister()
  }

  lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
    override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
      launcher?.unregister()
    }
  })

  launcher = activityResultRegistry.register(key, contract, callback)
  return launcher
}
