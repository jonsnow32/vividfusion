package cloud.app.vvf.common.exceptions

open class LoginRequiredException(
    open val extensionId: String,
    open val clientName: String
) : Exception("Login Required ($extensionId : $clientName)")
