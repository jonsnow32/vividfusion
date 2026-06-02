package cloud.app.vvf.ui.exception

sealed class AppException(
    override val cause: Throwable
) : Exception() {

    data class Other(
        override val cause: Throwable
    ) : AppException(cause) {
        override val message: String
            get() = cause.message ?: "Unknown error"
    }
}
