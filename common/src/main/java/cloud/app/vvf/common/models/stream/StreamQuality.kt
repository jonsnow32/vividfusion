package cloud.app.vvf.common.models.stream

import cloud.app.vvf.common.models.ImageHolder


enum class StreamQuality(value: Int?) {
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
  WebRip(16),
  Unknow(17);

  fun toImageHolder(): ImageHolder? {
    TODO("Not yet implemented")
  }
}

/**Add anything to here if you find a site that uses some specific naming convention*/
fun getQualityFromString(string: String?): StreamQuality? {
  val check = (string ?: return null).trim().lowercase().replace(" ", "")

  return when (check) {
    "cam" -> StreamQuality.Cam
    "camrip" -> StreamQuality.CamRip
    "hdcam" -> StreamQuality.HdCam
    "hdtc" -> StreamQuality.HdCam
    "hdts" -> StreamQuality.HdCam
    "highquality" -> StreamQuality.HQ
    "hq" -> StreamQuality.HQ
    "highdefinition" -> StreamQuality.HD
    "hdrip" -> StreamQuality.HD
    "hd" -> StreamQuality.HD
    "hdtv" -> StreamQuality.HD
    "rip" -> StreamQuality.CamRip
    "telecine" -> StreamQuality.Telecine
    "tc" -> StreamQuality.Telecine
    "telesync" -> StreamQuality.Telesync
    "ts" -> StreamQuality.Telesync
    "dvd" -> StreamQuality.DVD
    "dvdrip" -> StreamQuality.DVD
    "dvdscr" -> StreamQuality.DVD
    "blueray" -> StreamQuality.BlueRay
    "bluray" -> StreamQuality.BlueRay
    "blu" -> StreamQuality.BlueRay
    "fhd" -> StreamQuality.HD
    "br" -> StreamQuality.BlueRay
    "standard" -> StreamQuality.SD
    "sd" -> StreamQuality.SD
    "4k" -> StreamQuality.FourK
    "uhd" -> StreamQuality.UHD // may also be 4k or 8k
    "blue" -> StreamQuality.BlueRay
    "wp" -> StreamQuality.WorkPrint
    "workprint" -> StreamQuality.WorkPrint
    "webrip" -> StreamQuality.WebRip
    "webdl" -> StreamQuality.WebRip
    "web" -> StreamQuality.WebRip
    "hdr" -> StreamQuality.HDR
    "sdr" -> StreamQuality.SDR
    else -> null
  }
}
