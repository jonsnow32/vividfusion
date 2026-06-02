package cloud.app.vvf.common.exceptions

class UnauthorizedException(
    val userId: String,
    override val extensionId: String,
    override val clientName: String
) : LoginRequiredException(extensionId, clientName) {
    override val message: String
        get() = "Unauthorized ($userId : $extensionId : $clientName)"
}
