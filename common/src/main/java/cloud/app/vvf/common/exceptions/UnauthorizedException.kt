package cloud.app.vvf.common.exceptions

import cloud.app.vvf.common.models.ExtensionType

class UnauthorizedException(
    val userId: String,
    override val clientId: String,
    override val clientName: String,
    override val clientType: ExtensionType
) : LoginRequiredException(clientId, clientName, clientType) {
    override val message: String
        get() = "Unauthorized ($userId : $clientId : $clientName)"
}
