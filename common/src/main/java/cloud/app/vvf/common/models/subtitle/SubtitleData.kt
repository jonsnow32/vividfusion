package cloud.app.vvf.common.models.subtitle


import kotlinx.serialization.Serializable

@Serializable
data class SubtitleData(
  val name: String,
  val languageCode: String,
  val languageName: String,
  val subtitleUrl: String
)
