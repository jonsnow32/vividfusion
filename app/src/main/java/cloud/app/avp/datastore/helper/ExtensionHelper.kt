package cloud.app.avp.datastore.helper

import cloud.app.avp.datastore.DataStore
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.ExtensionMetadata

const val ExtensionFolder = "extensionDir"

fun DataStore.getExtension(type: ExtensionType, id: String): ExtensionMetadata? {
  return getKey<ExtensionMetadata>("$ExtensionFolder/${type.feature}/${id}", null)
}

