package cloud.app.vvf.datastore.helper

import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ExtensionMetadata

const val ExtensionFolder = "extensionDir"

fun DataStore.getExtension(type: ExtensionType, id: String): ExtensionMetadata? {
  return getKey<ExtensionMetadata>("$ExtensionFolder/${type.feature}/${id}", null)
}
