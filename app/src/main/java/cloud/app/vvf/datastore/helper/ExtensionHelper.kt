package cloud.app.vvf.datastore.helper

import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ExtensionMetadata

const val ExtensionFolder = "extensionDir"

fun DataStore.getExtension(id: String): ExtensionMetadata? {
  return getKey<ExtensionMetadata>("$ExtensionFolder/${id}", null)
}

fun DataStore.getExtensions(type: ExtensionType, id: String): List<ExtensionMetadata>? {
  return getKey<List<ExtensionMetadata>>("$ExtensionFolder/${type.feature}", null)
}

fun DataStore.createOrUpdateExtension(metadata: ExtensionMetadata) {
  return setKey("$ExtensionFolder/${metadata.className}", metadata)
}

fun DataStore.removeExtension(id: String) {
  return removeKey("$ExtensionFolder/${id}")
}

