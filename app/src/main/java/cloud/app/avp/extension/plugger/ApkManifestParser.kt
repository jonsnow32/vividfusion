package cloud.app.avp.extension.plugger

import cloud.app.common.helpers.ImportType
import tel.jeelpa.plugger.ManifestParser
import cloud.app.common.models.ExtensionMetadata
class ApkManifestParser(
    private val importType: ImportType
) : ManifestParser<AppInfo, ExtensionMetadata> {
    override fun parseManifest(data: AppInfo): ExtensionMetadata = with(data.appInfo.metaData) {
        fun get(key: String): String = getString(key)
            ?: error("$key not found in Metadata for ${data.appInfo.packageName}")

        ExtensionMetadata(
            path = data.path,
            className = get("class"),
            importType = importType,
            id = get("id"),
            name = get("name"),
            version = get("version"),
            description = get("description"),
            author = get("author"),
            authorUrl = getString("author_url"),
            iconUrl = getString("icon_url"),
            repoUrl = getString("repo_url"),
            updateUrl = getString("update_url"),
            enabled = getBoolean("enabled", true)
        )
    }
}
