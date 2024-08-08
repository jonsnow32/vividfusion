package cloud.app.avp.network.api.trakt.services.model.stats

data class Shows(
    val collected: Int,
    val comments: Int,
    val ratings: Int,
    val watched: Int
)
