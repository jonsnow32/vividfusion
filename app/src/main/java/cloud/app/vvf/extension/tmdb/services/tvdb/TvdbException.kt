package cloud.app.vvf.extension.tmdb.services.tvdb

open class TvdbException protected constructor(
    message: String?, throwable: Throwable?, private val service: Service?,
    private val itemDoesNotExist: Boolean
) : Exception(message, throwable) {
    constructor(message: String?) : this(message, null, Service.TVDB, false)

    constructor(message: String?, throwable: Throwable?) : this(
        message,
        throwable,
        Service.TVDB,
        false
    )

    constructor(message: String?, itemDoesNotExist: Boolean) : this(
        message,
        null,
        Service.TVDB,
        itemDoesNotExist
    )

    /**
     * If not `null` the service that interacting with failed.
     */
    fun service(): Service? {
        return service
    }

    /**
     * If the TheTVDB item does not exist (a HTTP 404 response was returned).
     */
    fun itemDoesNotExist(): Boolean {
        return itemDoesNotExist
    }

    enum class Service {
        TVDB,
        TRAKT,
        HEXAGON,
        DATA
    }
}
