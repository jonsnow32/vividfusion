package cloud.app.plugger.repos.file

data class FileConfig(
  val path: String,
  val extension: String, // to be used as ".example" / ".jar" / ".zip"
)
