package cloud.app.vvf.common.exceptions

import cloud.app.vvf.common.models.ExtensionType

class MissingApiKeyException(
  val clientId: String,
  val clientName: String,
  val clientType: ExtensionType,
  val apiKeyPref: String
) : Exception("Missing Api key ($clientId : $clientName)")

