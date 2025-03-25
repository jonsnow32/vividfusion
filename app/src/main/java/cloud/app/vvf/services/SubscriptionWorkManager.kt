package cloud.app.vvf.services

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build.VERSION.SDK_INT
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import cloud.app.vvf.VVFApplication.Companion.createNotificationChannel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import cloud.app.vvf.R
import cloud.app.vvf.utils.colorFromAttribute

const val SUBSCRIPTION_CHANNEL_ID = "movie.subscriptions"
const val SUBSCRIPTION_WORK_NAME = "work_subscription"
const val SUBSCRIPTION_CHANNEL_NAME = "Subscriptions"
const val SUBSCRIPTION_CHANNEL_DESCRIPTION = "Notifications for new episodes on subscribed shows"
const val SUBSCRIPTION_NOTIFICATION_ID = 237839 // Random unique

@HiltWorker
class SubscriptionWorkManager @AssistedInject constructor(
  @Assisted val context: Context,
  @Assisted workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

  companion object {
    fun enqueuePeriodicWork(context: Context?) {
      if (context == null) return

      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val periodicSyncDataWork =
        PeriodicWorkRequest.Builder(SubscriptionWorkManager::class.java, 6, TimeUnit.HOURS)
          .addTag(SUBSCRIPTION_WORK_NAME)
          .setConstraints(constraints)
          .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        SUBSCRIPTION_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        periodicSyncDataWork
      )

      // Uncomment below for testing

//            val oneTimeSyncDataWork =
//                OneTimeWorkRequest.Builder(SubscriptionWorkManager::class.java)
//                    .addTag(SUBSCRIPTION_WORK_NAME)
//                    .setConstraints(constraints)
//                    .build()
//
//            WorkManager.getInstance(context).enqueue(oneTimeSyncDataWork)
    }
  }

  private val progressNotificationBuilder =
    NotificationCompat.Builder(context, SUBSCRIPTION_CHANNEL_ID)
      .setAutoCancel(false)
      .setColorized(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setColor(context.colorFromAttribute(androidx.appcompat.R.attr.colorPrimary))
      .setContentTitle(context.getString(R.string.subscription_in_progress_notification))
      .setSmallIcon(com.google.android.gms.cast.framework.R.drawable.quantum_ic_refresh_white_24)
      .setProgress(0, 0, true)

  private val updateNotificationBuilder =
    NotificationCompat.Builder(context, SUBSCRIPTION_CHANNEL_ID)
      .setColorized(true)
      .setOnlyAlertOnce(true)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setColor(context.colorFromAttribute(androidx.appcompat.R.attr.colorPrimary))
      .setSmallIcon(R.drawable.ic_launcher_foreground)

  private val notificationManager: NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  private fun updateProgress(max: Int, progress: Int, indeterminate: Boolean) {
    notificationManager.notify(
      SUBSCRIPTION_NOTIFICATION_ID, progressNotificationBuilder
        .setProgress(max, progress, indeterminate)
        .build()
    )
  }

  override suspend fun doWork(): Result {
    try {
      context.createNotificationChannel(
        SUBSCRIPTION_CHANNEL_ID,
        SUBSCRIPTION_CHANNEL_NAME,
        SUBSCRIPTION_CHANNEL_DESCRIPTION
      )
      val foregroundInfo = if (SDK_INT >= 29)
        ForegroundInfo(
          SUBSCRIPTION_NOTIFICATION_ID,
          progressNotificationBuilder.build(),
          FOREGROUND_SERVICE_TYPE_DATA_SYNC
        ) else ForegroundInfo(
        SUBSCRIPTION_NOTIFICATION_ID,
        progressNotificationBuilder.build(),
      )
      setForeground(foregroundInfo)

      //todo add logic here
    } catch (e: Exception) {
      return Result.failure()
    }
    return Result.success()
  }
}
