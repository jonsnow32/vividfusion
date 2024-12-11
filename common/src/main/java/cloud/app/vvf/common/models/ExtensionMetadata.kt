package cloud.app.vvf.common.models

import cloud.app.vvf.common.helpers.ImportType
import kotlinx.serialization.Serializable

@Serializable
data class ExtensionMetadata(
  val className: String,
  val path: String,
  val importType: ImportType,
  val id: String,
  var name: String,
  var version: String,
  var description: String,
  var author: String,
  var authorUrl: String? = null,
  var iconUrl: String? = null,
  var repoUrl: String? = null,
  var updateUrl: String? = null,
  var enabled: Boolean = true,
  var types: List<ExtensionType>? = null
)
