package cloud.app.vvf.extension.builtIn.providers.subtitles

import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.subtitle.SubtitleOrigin
import cloud.app.vvf.extension.builtIn.providers.SubtitleProvider
import kotlinx.coroutines.delay

class DummySubtitles : SubtitleProvider {
  override suspend fun loadSubtitles(
    httpHelper: HttpHelper,
    searchItem: SearchItem,
    callback: suspend (SubtitleData) -> Unit
  ) {
    val subtitles = listOf(
      SubtitleData(
        name = "TTML positioning",
        mimeType = "application/ttml+xml",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ttml/netflix_ttml_sample.xml",
        headers = mapOf(),
        isHearingImpaired = true
      ),
      SubtitleData(
        name = "TTML Japanese features",
        mimeType = "application/ttml+xml",
        languageCode = "ja",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ttml/japanese-ttml.xml",
        headers = mapOf()
      ),
      SubtitleData(
        name = "TTML Netflix Japanese examples (IMSC1.1)",
        mimeType = "application/ttml+xml",
        languageCode = "ja",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ttml/netflix_japanese_ttml.xml",
        headers = mapOf()
      ),
      SubtitleData(
        name = "WebVTT positioning",
        mimeType = "text/vtt",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/numeric-lines.vtt",
        headers = mapOf()
      ),
      SubtitleData(
        name = "WebVTT Japanese features",
        mimeType = "text/vtt",
        languageCode = "ja",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/japanese.vtt",
        headers = mapOf()
      ),
      SubtitleData(
        name = "SubStation Alpha positioning",
        mimeType = "text/x-ssa",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ssa/test-subs-position.ass",
        headers = mapOf()
      ),
      SubtitleData(
        name = "SubStation Alpha styling",
        mimeType = "text/x-ssa",
        languageCode = "en",
        origin = SubtitleOrigin.URL,
        url = "https://storage.googleapis.com/exoplayer-test-media-1/ssa/test-subs-styling.ass",
        headers = mapOf()
      )
    )


    for (i in 1..subtitles.size) {
      delay(1000)
      callback.invoke(subtitles.get(i - 1))
    }
  }

}
