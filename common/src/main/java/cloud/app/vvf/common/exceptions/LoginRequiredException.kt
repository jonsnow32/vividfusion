package cloud.app.vvf.common.exceptions

import cloud.app.vvf.common.models.ExtensionType

open class LoginRequiredException(
    open val clientId: String,
    open val clientName: String,
    open val clientType: ExtensionType
) : Exception("Login Required ($clientId : $clientName)")
