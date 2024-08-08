package cloud.app.plugger.repos.installedApk

data class InstalledApkConfig(
  val packagePrefix: String,
  val featureName: String = "$packagePrefix.extension",
  val metadataSourceClassTag: String = "$packagePrefix.sourceclass",
)
