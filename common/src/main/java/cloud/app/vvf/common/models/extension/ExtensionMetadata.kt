package cloud.app.vvf.common.models.extension

import cloud.app.vvf.common.helpers.ImportType
import kotlinx.serialization.Serializable

@Serializable
data class ExtensionMetadata(
  val className: String,
  val path: String,
  val importType: ImportType,
  var types: List<ExtensionType>,

  var name: String,
  var version: String,
  var description: String,
  var author: String,
  var authorUrl: String? = null,
  var iconUrl: String? = null,
  var repoUrl: String? = null,
  var updateUrl: String? = null,

  var enabled: Boolean = true,
  var lastUpdated: Long = -1,
  var rating: Int = 0,
  var preservedPackages: List<String> = emptyList()
) {

  init {
    require(types.isNotEmpty()) { "types must contain at least one ExtensionType" }
  }
}
