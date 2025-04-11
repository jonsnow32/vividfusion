package cloud.app.vvf.common.exceptions

import cloud.app.vvf.common.models.extension.ExtensionType

class MissingApiKeyException(
  val extensionId: String,
  val clientName: String,
  val clientType: ExtensionType,
  val apiKeyPref: String
) : Exception("Missing Api key ($extensionId : $clientName)")

