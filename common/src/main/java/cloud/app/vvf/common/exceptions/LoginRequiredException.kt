package cloud.app.vvf.common.exceptions

import cloud.app.vvf.common.models.extension.ExtensionType

open class LoginRequiredException(
    open val extensionId: String,
    open val clientName: String,
    open val clientType: ExtensionType
) : Exception("Login Required ($extensionId : $clientName)")
