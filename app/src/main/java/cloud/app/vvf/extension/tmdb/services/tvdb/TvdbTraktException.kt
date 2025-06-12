package cloud.app.vvf.extension.tmdb.services.tvdb

/**
 * Thrown when a [] trakt operation fails.
 */
class TvdbTraktException(message: String?) : TvdbException(message, null, Service.TRAKT, false)
