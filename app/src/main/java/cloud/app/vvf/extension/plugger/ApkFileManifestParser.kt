package cloud.app.vvf.extension.plugger

import android.content.pm.PackageManager
import tel.jeelpa.plugger.ManifestParser
import cloud.app.vvf.common.models.ExtensionMetadata
import java.io.File

class ApkFileManifestParser(
    private val packageManager: PackageManager,
    private val apkManifestParser: ManifestParser<AppInfo, ExtensionMetadata>
) : ManifestParser<File, ExtensionMetadata> {
    override fun parseManifest(data: File): ExtensionMetadata {
        return apkManifestParser.parseManifest(
            AppInfo(
                data.path,
                packageManager
                    .getPackageArchiveInfo(data.path, ApkPluginSource.PACKAGE_FLAGS)!!
                    .applicationInfo!!
            )
        )
    }
}
