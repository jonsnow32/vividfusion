package cloud.app.avp.network.api.trakt.services.model.stats

data class UserStats(
    val episodes: Episodes,
    val movies: Movies,
    val network: Network,
    val ratings: Ratings,
    val seasons: Seasons,
    val shows: Shows
)
