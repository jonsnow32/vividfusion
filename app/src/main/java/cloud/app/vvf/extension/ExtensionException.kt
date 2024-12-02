package cloud.app.vvf.extension

import cloud.app.vvf.common.models.ExtensionType


class ExtensionLoadingException(
  val type: ExtensionType,
  override val cause: Throwable
) : Exception("Failed to load extension of type: $type")

class InvalidExtensionListException(override val cause: Throwable) : Exception(cause)
