package cloud.app.vvf.services

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cloud.app.vvf.R
import cloud.app.vvf.VVFApplication.Companion.createNotificationChannel
import cloud.app.vvf.utils.FileHelper
import cloud.app.vvf.utils.KUniFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


const val BACKUP_CHANNEL_ID = "vvf.backups"
const val BACKUP_WORK_NAME = "work_backup"
const val BACKUP_CHANNEL_NAME = "Backups"
const val BACKUP_CHANNEL_DESCRIPTION = "Notifications for background backups"
const val BACKUP_NOTIFICATION_ID = 3429584

@HiltWorker
class BackupWorker @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted params: WorkerParameters,
  val sharedPreferences: SharedPreferences,
  val fileHelper: FileHelper
) :
  CoroutineWorker(context, params) {
  companion object {
    fun enqueuePeriodicWork(context: Context?, intervalHours: Long) {
      if (context == null) return

      if (intervalHours == 0L) {
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
        return
      }

      val constraints = Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .build()

      val periodicSyncDataWork =
        PeriodicWorkRequest.Builder(
          BackupWorker::class.java,
          intervalHours,
          TimeUnit.HOURS
        )
          .addTag(BACKUP_WORK_NAME)
          .setConstraints(constraints)
          .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        BACKUP_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        periodicSyncDataWork
      )

      // Uncomment below for testing
//      val oneTimeBackupWork =
//        OneTimeWorkRequest.Builder(BackupWorker::class.java)
//          .addTag(BACKUP_WORK_NAME)
//          .setConstraints(constraints)
//          .build()
//
//      WorkManager.getInstance(context).enqueue(oneTimeBackupWork)
    }
  }

  private val backupNotificationBuilder =
    NotificationCompat.Builder(context, BACKUP_CHANNEL_ID)
      .setColorized(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setAutoCancel(true)
      .setContentTitle(context.getString(R.string.backup))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setSmallIcon(R.drawable.ic_launcher_background)

  override suspend fun doWork(): Result {
    context.createNotificationChannel(
      BACKUP_CHANNEL_ID,
      BACKUP_CHANNEL_NAME,
      BACKUP_CHANNEL_DESCRIPTION
    )

    val foregroundInfo = if (SDK_INT >= 29)
      ForegroundInfo(
        BACKUP_NOTIFICATION_ID, backupNotificationBuilder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC
      ) else ForegroundInfo(BACKUP_NOTIFICATION_ID, backupNotificationBuilder.build())
    setForeground(foregroundInfo)


    return withContext(Dispatchers.IO) {
      // Define backup file
      val backupFilePath = sharedPreferences.getString(context.getString(R.string.pref_backup_path), null)
      val backupFile = KUniFile.fromUri(context, Uri.parse(backupFilePath)) ?: return@withContext Result.failure()


      // Or backup all SharedPreferences
      val allPrefs = fileHelper.getAllSharedPrefsNames()
      val backupAllSuccess =
        fileHelper.backupSharedPreferencesToJson(allPrefs, backupFile)

      if (backupAllSuccess)
        Result.success()
      else
        Result.failure()
    }
  }

}
