package cloud.app.vvf.common.exceptions

import cloud.app.vvf.common.models.extension.ExtensionType

class UnauthorizedException(
    val userId: String,
    override val extensionId: String,
    override val clientName: String,
    override val clientType: ExtensionType
) : LoginRequiredException(extensionId, clientName, clientType) {
    override val message: String
        get() = "Unauthorized ($userId : $extensionId : $clientName)"
}
