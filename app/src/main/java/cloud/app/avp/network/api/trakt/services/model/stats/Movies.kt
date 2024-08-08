package cloud.app.avp.network.api.trakt.services.model.stats

data class Movies(
    val collected: Int,
    val comments: Int,
    val minutes: Int,
    val plays: Int,
    val ratings: Int,
    val watched: Int
)
