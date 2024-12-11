package cloud.app.vvf.extension

import cloud.app.vvf.common.models.ExtensionMetadata


class ExtensionLoadingException(
  val metadata: ExtensionMetadata?,
  override val cause: Throwable
) : Exception("Failed to load extension of type: ${metadata?.id} cause ${cause.message}")

class InvalidExtensionListException(override val cause: Throwable) : Exception(cause)
