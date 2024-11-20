package cloud.app.avp.extension.plugger

import android.content.pm.ApplicationInfo

data class AppInfo(
    val path: String,
    val appInfo: ApplicationInfo
)
