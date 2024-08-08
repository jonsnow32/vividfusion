package cloud.app.avp.network.api.openSubtitle.models

data class FeatureDetails(
    val feature_id: Int,
    val feature_type: String,
    val imdb_id: Int,
    val movie_name: String,
    val title: String,
    val tmdb_id: Int,
    val year: Int
)
