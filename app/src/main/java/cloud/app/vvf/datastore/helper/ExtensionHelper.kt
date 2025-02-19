package cloud.app.vvf.datastore.helper

import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.common.models.ExtensionMetadata
import kotlinx.serialization.Serializable

const val ExtensionFolder = "extensionDir"

fun DataStore.getExtension(className: String): ExtensionMetadata? {
  return getExtensions()?.firstOrNull { it.className == className }
}

fun DataStore.getExtensions(): List<ExtensionMetadata>? {
  return getKeys<ExtensionMetadata>("$ExtensionFolder/", null)
}

fun DataStore.saveExtensions(extensions: List<ExtensionMetadata>) {
  for (extension in extensions) {
    setKey("$ExtensionFolder/${extension.className}", extension)
  }
}

fun DataStore.saveExtension(extension: ExtensionMetadata) {
  extension.lastUpdated = System.currentTimeMillis()
  return setKey("$ExtensionFolder/${extension.className}", extension)
}

fun DataStore.getCurrentDBExtension(): ExtensionMetadata? {
  return getKey<ExtensionMetadata>("$ExtensionFolder/defaultDB/", null)
}

fun DataStore.setCurrentDBExtension(extension: ExtensionMetadata): Boolean {
  setKey("$ExtensionFolder/defaultDB/", extension)
  return true
}


