package cloud.app.vvf.extension.plugger

import android.content.pm.ApplicationInfo

data class ApkFileInfo(
    val path: String,
    val appInfo: ApplicationInfo
)
