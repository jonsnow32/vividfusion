package cloud.app.common.models.subtitle

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubtitleData(
  val name: String,
  val languageCode: String,
  val languageName: String,
  val subtitleUrl: String
) : Parcelable
