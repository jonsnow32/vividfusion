package cloud.app.vvf.network.api.trakt.services.model.stats

data class Episodes(
    val collected: Int,
    val comments: Int,
    val minutes: Int,
    val plays: Int,
    val ratings: Int,
    val watched: Int
)
