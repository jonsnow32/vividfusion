package cloud.app.vvf.ui.exception

import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.common.helpers.ClientException

sealed class AppException(
    override val cause: Throwable
) : Exception() {

    abstract val extensionId: Extension<*>

    open class LoginRequired(
        override val cause: Throwable,
        override val extensionId: Extension<*>
    ) : AppException(cause)

    data class Unauthorized(
      override val cause: Throwable,
      override val extensionId: Extension<*>,
      val userId: String
    ) : LoginRequired(cause, extensionId)

    data class NotSupported(
      override val cause: Throwable,
      override val extensionId: Extension<*>,
      val operation: String
    ) : AppException(cause) {
        override val message: String
            get() = "$operation is not supported in ${extensionId.name}"
    }

    data class Other(
        override val cause: Throwable,
        override val extensionId: Extension<*>
    ) : AppException(cause) {
        override val message: String
            get() = "${cause.message} error in ${extensionId.name}"
    }

    companion object {
        fun Throwable.toAppException(extension: Extension<*>): AppException = when (this) {
            is ClientException.Unauthorized -> Unauthorized(this, extension, userId)
            is ClientException.LoginRequired -> LoginRequired(this, extension)
            is ClientException.NotSupported -> NotSupported(this, extension, operation)
            else -> Other(this, extension)
        }
    }
}
