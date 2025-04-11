package cloud.app.vvf.common.models.stream

import cloud.app.vvf.common.models.ImageHolder


enum class Resolution(value: Int?) {
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

fun getResolutionFromString(string: String?): Resolution? {
  val check = (string ?: return null).trim().lowercase().replace(" ", "")

  return when (check) {
    "cam" -> Resolution.Cam
    "camrip" -> Resolution.CamRip
    "hdcam" -> Resolution.HdCam
    "hdtc" -> Resolution.HdCam
    "hdts" -> Resolution.HdCam
    "highquality" -> Resolution.HQ
    "hq" -> Resolution.HQ
    "highdefinition" -> Resolution.HD
    "hdrip" -> Resolution.HD
    "hd" -> Resolution.HD
    "hdtv" -> Resolution.HD
    "rip" -> Resolution.CamRip
    "telecine" -> Resolution.Telecine
    "tc" -> Resolution.Telecine
    "telesync" -> Resolution.Telesync
    "ts" -> Resolution.Telesync
    "dvd" -> Resolution.DVD
    "dvdrip" -> Resolution.DVD
    "dvdscr" -> Resolution.DVD
    "blueray" -> Resolution.BlueRay
    "bluray" -> Resolution.BlueRay
    "blu" -> Resolution.BlueRay
    "fhd" -> Resolution.HD
    "br" -> Resolution.BlueRay
    "standard" -> Resolution.SD
    "sd" -> Resolution.SD
    "4k" -> Resolution.FourK
    "uhd" -> Resolution.UHD // may also be 4k or 8k
    "blue" -> Resolution.BlueRay
    "wp" -> Resolution.WorkPrint
    "workprint" -> Resolution.WorkPrint
    "webrip" -> Resolution.WebRip
    "webdl" -> Resolution.WebRip
    "web" -> Resolution.WebRip
    "hdr" -> Resolution.HDR
    "sdr" -> Resolution.SDR
    else -> null
  }
}
