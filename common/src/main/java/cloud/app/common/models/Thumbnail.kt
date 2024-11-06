package cloud.app.common.models


import kotlinx.serialization.Serializable

@Serializable
open class Thumbnail(
  open var id: Int?,
  open val name: String,
  open val url: String,
  open val source: String,
  open var type: MediaType?,
  open var poster: ImageHolder?,
  open val subtitles: List<SubtitleData> = listOf(),
  open var quality: VideoQuality?
)


enum class VideoQuality(value: Int?) {
  //https://en.wikipedia.org/wiki/Pirated_movie_release_types
  Cam(1),
  CamRip(2),
  HdCam(3),
  Telesync(4), // TS
  WorkPrint(5),
  Telecine(6), // TC
  HQ(7),
  HD(8),
  HDR(9), // high dynamic range
  BlueRay(10),
  DVD(11),
  SD(12),
  FourK(13),
  UHD(14),
  SDR(15), // standard dynamic range
  WebRip(16)
}

enum class MediaType(value: Int?) {
  Movie(1),
  AnimeMovie(2),
  TvSeries(3),
  Cartoon(4),
  Anime(5),
  OVA(6),
  Torrent(7),
  Documentary(8),
  AsianDrama(9),
  Live(10),
  NSFW(11),
  Others(12),
  Music(13),
  AudioBook(14),

  /** Wont load the built in player, make your own interaction */
  CustomMedia(15),
}
