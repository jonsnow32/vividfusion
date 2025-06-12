package cloud.app.vvf.extension.tmdb.services.tvdb


class TvdbCloudException(message: String?, throwable: Throwable?) :
    TvdbException(message, throwable, Service.HEXAGON, false)
