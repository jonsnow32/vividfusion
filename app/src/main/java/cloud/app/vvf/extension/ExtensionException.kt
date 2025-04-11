package cloud.app.vvf.extension

import cloud.app.vvf.common.models.extension.ExtensionMetadata


class ExtensionLoadingException(
  val metadata: ExtensionMetadata?,
  override val cause: Throwable
) : Exception("Failed to load extension of type: ${metadata?.className} cause ${cause.message}")

class InvalidExtensionListException(override val cause: Throwable) : Exception(cause)

data class RequiredExtensionsException(
  val name: String,
  val requiredExtensions: List<String>
) : Exception("Required extensions not found")
