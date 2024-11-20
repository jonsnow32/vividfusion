package cloud.app.avp.extension

import cloud.app.common.models.ExtensionType


class ExtensionLoadingException(
  val type: ExtensionType,
  override val cause: Throwable
) : Exception("Failed to load extension of type: $type")
