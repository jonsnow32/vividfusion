package cloud.app.vvf.datastore.helper

import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ExtensionMetadata

const val ExtensionFolder = "extensionDir"

fun DataStore.getExtension(type: ExtensionType, id: String): ExtensionMetadata? {
  return getKey<ExtensionMetadata>("$ExtensionFolder/${type.feature}/${id}", null)
}

fun DataStore.getExtensions(type: ExtensionType, id: String): List<ExtensionMetadata>? {
  return getKey<List<ExtensionMetadata>>("$ExtensionFolder/${type.feature}", null)
}

fun DataStore.createOrUpdateExtension(type: ExtensionType, metadata: ExtensionMetadata) {
  return setKey("$ExtensionFolder/${type.feature}/${metadata.id}", metadata)
}

fun DataStore.removeExtension(type: ExtensionType, id: String) {
  return removeKey("$ExtensionFolder/${type.feature}/${id}")
}

