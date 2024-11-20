package cloud.app.common.models

import cloud.app.common.helpers.ImportType

data class ExtensionMetadata(
  val className: String,
  val path: String,
  val importType: ImportType,
  val id: String,
  val name: String,
  val version: String,
  val description: String,
  val author: String,
  val authorUrl: String? = null,
  val iconUrl: String? = null,
  val repoUrl: String? = null,
  val updateUrl: String? = null,
  val enabled: Boolean = true
)
