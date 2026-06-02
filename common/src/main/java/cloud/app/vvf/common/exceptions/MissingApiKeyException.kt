package cloud.app.vvf.common.exceptions

class MissingApiKeyException(
  val extensionId: String,
  val clientName: String,
  val apiKeyPref: String
) : Exception("Missing Api key ($extensionId : $clientName)")
