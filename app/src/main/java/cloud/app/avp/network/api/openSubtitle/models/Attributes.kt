package cloud.app.avp.network.api.openSubtitle.models

data class Attributes(
    val ai_translated: Boolean,
    val comments: String,
    val download_count: Int,
    val feature_details: FeatureDetails,
    val files: List<File>,
    val foreign_parts_only: Boolean,
    val fps: Double,
    val from_trusted: Boolean,
    val hd: Boolean,
    val hearing_impaired: Boolean,
    val language: String,
    val legacy_subtitle_id: Int,
    val machine_translated: Boolean,
    val new_download_count: Int,
    val ratings: Double,
    val related_links: List<RelatedLink>,
    val release: String,
    val subtitle_id: String,
    val upload_date: String,
    val uploader: Uploader,
    val url: String,
    val votes: Int
)
