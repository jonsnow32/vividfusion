package cloud.app.vvf.datastore.app.helper

import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.datastore.app.AppDataStore

const val ExtensionFolder = "extensionDir"

fun AppDataStore.getExtension(className: String): ExtensionMetadata? {
  return getExtensions()?.firstOrNull { it.className == className }
}

fun AppDataStore.getExtensions(): List<ExtensionMetadata>? {
  return getKeys<ExtensionMetadata>("$ExtensionFolder/", null)
}

fun AppDataStore.saveExtensions(extensions: List<ExtensionMetadata>) {
  for (extension in extensions) {
    setKey("$ExtensionFolder/${extension.className}", extension)
  }
}

fun AppDataStore.saveExtension(extension: ExtensionMetadata) {
  extension.lastUpdated = System.currentTimeMillis()
  return setKey("$ExtensionFolder/${extension.className}", extension)
}

fun AppDataStore.getCurrentDBExtension(): ExtensionMetadata? {
  return getKey<ExtensionMetadata>("$ExtensionFolder/defaultDB/", null)
}

fun AppDataStore.setCurrentDBExtension(extension: ExtensionMetadata): Boolean {
  setKey("$ExtensionFolder/defaultDB/", extension)
  return true
}


