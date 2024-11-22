package cloud.app.avp.extension.provider

import android.util.Log
import cloud.app.avp.extension.Constants
import cloud.app.avp.plugin.getlink.BaseScaper
import cloud.app.avp.utils.TimeUtils
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.helpers.network.Utils
import cloud.app.common.helpers.network.utils.AES256Cryptor
import cloud.app.common.helpers.network.utils.MD5Utils
import cloud.app.common.models.SubtitleData
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.stream.StreamData
import com.domain.usecases.stream.Utils.RegexUtils
import org.jsoup.nodes.Element


class M4UFree(val httpHelper: HttpHelper) : BaseScaper() {
  val BASE_URL: String get() = "https://m4ufree.se"

  override val name: String
    get() = "M4UFree"

  override suspend fun getMovieStream(
    movie: Movie,
    subtitleCallback: (SubtitleData) -> Unit,
    linkCallback: (StreamData) -> Unit
  ) {
    val url = search(
      movie.generalInfo.title,
      TimeUtils.getYear(movie.generalInfo.releaseDateMsUTC!!).toString(),
      -1,
      -1
    )
    if (url != null) {

      linkCallback(
        StreamData(
          "sample",
          resolvedUrl =  "sample",
          fileName =  "sample"
        )
      )
      getLink(url, -1, -1, movie.generalInfo.title, subtitleCallback, linkCallback)
    }
  }

  override suspend fun getEpisodeStream(
    episode: Episode,
    subtitleCallback: (SubtitleData) -> Unit,
    linkCallback: (StreamData) -> Unit
  ) {
    TODO("Not yet implemented")
  }

  suspend fun search(
    name: String,
    year: String,
    season: Int,
    episode: Int

  ): String? {

    val valueOf: String;
    val isMovies: Boolean = season < 0 || episode < 0;
    if (isMovies) {
      valueOf = year;
    } else {
      valueOf =
        "S" + Utils.fixEpisode(season.toString()) + "E" + Utils.fixEpisode(
          episode.toString()
        );
    }
    //HttpHelper.m13040().m13048(BASE_URL);

    val hashMap: HashMap<String, String> = HashMap()
    hashMap["accept"] =
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
    hashMap["referer"] = BASE_URL + "/"
    hashMap["origin"] = BASE_URL

    val keyword: String =
      Utils.replaceAllkeywithTarget(name.lowercase() + " " + valueOf.lowercase(), "-")
    var url = "";

    var searchUrl = String.format("%s/search/%s.html", BASE_URL, keyword);
    var doc = httpHelper.get(searchUrl).document;
    val iterator: Iterator<*> = doc.select("div.row div.item").select("a[href]").iterator()
    while (iterator.hasNext()) {
      val elsA = iterator.next() as Element;
      val title = elsA.attr("title")
      if (Utils.replaceAllkeywithTarget(title.lowercase(), "").startsWith(
          Utils.replaceAllkeywithTarget(
            name.lowercase() + valueOf.lowercase(),
            ""
          )
        )
      ) {
        url = elsA!!.attr("href")
        if (!url.contains("http")) {
          url = BASE_URL + "/" + url;
        } else if (url.startsWith("/")) {
          url = BASE_URL + url;
        }
        return url;
      }

    }

    return url;

  }

  suspend fun getLink(
    url: String,
    season: Int,
    episode: Int,
    filename: String,
    subtitleCallback: (SubtitleData) -> Unit,
    linkCallback: (StreamData) -> Unit
  ) {
    //url = dataurl;
    val isMovies: Boolean = season < 0 || episode < 0;
    var newUrl = url
    var response = httpHelper.get(newUrl);
    var document = response.document;
    var html: String? = response.text


    val token: String = document.select("meta[name=csrf-token]").attr("content")
    val hashMap2: HashMap<String, String> = Utils.getHashXMLHttpRequest()
    val playhaeder: HashMap<String, String> = Utils.getHashXMLHttpRequest()
    hashMap2["Accept"] = "*/*"
//        hashMap2.put("Host",BASE_URL.replace("http://",""));
    //        hashMap2.put("Host",BASE_URL.replace("http://",""));
    hashMap2["Origin"] = BASE_URL
    hashMap2["Referer"] = newUrl

    playhaeder["Accept"] = "*/*"
    playhaeder["User-Agent"] = Constants.USER_AGENT
    httpHelper.loadCookieForRequest(newUrl)?.let {
      hashMap2.put("Cookie", it)
    };
    Log.d("M4UFree", "getLink hashMap2: ${hashMap2.toString()}")
    val iterator: Iterator<*> =
      document.select("div.le-server").select("span[data][class][id]").iterator()
    while (iterator.hasNext()) {
      val elsA = iterator.next() as Element;
      var playdata = elsA.attr("data")
      Log.d("M4UFree", "getLink playdata: $playdata")
//            if (!isMovies) {
//                var matcheps = elsA.attr("episode-data")
//                matcheps = RegexUtils.getGroup(matcheps, "(\\d+)", 1)
//                if (matcheps.isEmpty() || !matcheps.equals( episode.toString())) continue
//            }
      html = httpHelper.post(
        BASE_URL + "/ajax",
        //json = "m4u=" + Utils.URLEncoder(playdata) + "&_token=" + token,
        data = mapOf(
          "m4u" to playdata,
          "_token" to token
        ),
        headers = hashMap2
      ).text
      Log.d("M4UFree", "getLink: $html")
      if (!html.isNullOrEmpty()) {
        var iframe =
          RegexUtils.getGroup(html, "<iframe[^>]+src=['\"]([^'\"]+)['\"][^>]*>", 1);
        if (iframe.isEmpty()) {
          iframe =
            RegexUtils.getGroup(html, "['\"]?file['\"]?\\s*:\\s*['\"]?([^'\"]+)", 1);
        }
        if (!iframe.isNullOrEmpty()) {
          if (iframe.startsWith("//"))
            iframe = "https:" + iframe
          Log.d("M4UFree", "getLink iframe: $iframe")
          if (iframe.contains("playm4u")) {
            html = httpHelper.get(iframe, referer = "$BASE_URL/").text;
            if (!html.isNullOrEmpty()) {
              val idfile: String =
                RegexUtils.getGroup(
                  html,
                  "idfile(?:\\w+|)\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                  1
                )
              val idUser: String =
                RegexUtils.getGroup(
                  html,
                  "idUser(?:\\w+|)\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                  1
                )
              var idfile_dec = f0002(idfile, "jcLycoRJT6OWjoWspgLMOZwS3aSS0lEn")
              var idUser_dec = f0002(idUser, "PZZ3J3LDbLT0GY7qSA5wW5vchqgpO36O")

              var DOMAIN_API: String =
                RegexUtils.getGroup(
                  html,
                  "DOMAIN_API(?:\\w+|)\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                  1
                )
              if (DOMAIN_API.startsWith("//"))
                DOMAIN_API = "https:" + DOMAIN_API
              if (!DOMAIN_API.endsWith("/"))
                DOMAIN_API += "/"

              val hashMap1: HashMap<String, String> = HashMap()
              hashMap1["origin"] = Utils.getBaseUrl(iframe)
              hashMap1["referer"] = Utils.getBaseUrl(iframe) + "/"
              hashMap1["user-agent"] = Constants.USER_AGENT
              if (idfile.isNotEmpty() && idUser.isNotEmpty()) {
                var plf = "MacIntel";// list.random()
                var domain_ref = BASE_URL;
                //{"idfile":"66fae90526b30eda78ea64aa","iduser":"642bce856edd7cab511931a2","domain_play":"https://m4ufree.se","platform":"MacIntel","hlsSupport":true,"jwplayer":{"Browser":{"androidNative":false,"chrome":true,"edge":false,"facebook":false,"firefox":false,"ie":false,"msie":false,"safari":false,"version":{"version":"129.0.0.0","major":129,"minor":0}},"OS":{"android":false,"iOS":false,"mobile":false,"mac":true,"iPad":false,"iPhone":false,"windows":false,"tizen":false,"tizenApp":false,"version":{"version":"10_15_7","major":10,"minor":15}},"Features":{"iframe":true,"passiveEvents":true,"backgroundLoading":true}}}

                var json = String.format(
                  "{\"idfile\":\"%s\",\"iduser\":\"%s\",\"domain_play\":\"%s\",\"platform\":\"%s\",\"hlsSupport\":true,\"jwplayer\":{\"Browser\":{\"androidNative\":false,\"chrome\":true,\"edge\":false,\"facebook\":false,\"firefox\":false,\"ie\":false,\"msie\":false,\"safari\":false,\"version\":{\"version\":\"129.0.0.0\",\"major\":129,\"minor\":0}},\"OS\":{\"android\":false,\"iOS\":false,\"mobile\":false,\"mac\":true,\"iPad\":false,\"iPhone\":false,\"windows\":false,\"tizen\":false,\"tizenApp\":false,\"version\":{\"version\":\"10_15_7\",\"major\":10,\"minor\":15}},\"Features\":{\"iframe\":true,\"passiveEvents\":true,\"backgroundLoading\":true}}}",
                  idfile_dec,
                  idUser_dec,
                  domain_ref,
                  plf,
                  plf
                )
                var data = f0001(json, "vlVbUQhkOhoSfyteyzGeeDzU0BHoeTyZ")

                html = httpHelper.post(
                  "${DOMAIN_API}playiframe",
                  data = mapOf(
                    //"namekey" to NameKeyV3,
                    //"token" to ggtoken,
                    // "referrer" to "$BASE_URL/",
                    "data" to (data + "|" + MD5Utils.StringtoHex(data + "KRWN3AdgmxEMcd2vLN1ju9qKe8Feco5h")
                      .toString())
                  ),
                  headers = hashMap1
                ).text
              }
              var streamLink =
                RegexUtils.getGroup(
                  html!!,
                  "data['\"]\\s*:\\s*['\"]([^'\"]+[^'\"])['\"]",
                  1
                )
              if (!streamLink.isNullOrEmpty() && !streamLink.startsWith("http")) {
                streamLink = f0002(
                  streamLink,
                  "oJwmvmVBajMaRCTklxbfjavpQO7SZpsL"
                )
              }
              if (!streamLink.isNullOrEmpty()) {
                linkCallback(
                  StreamData(
                    iframe,
                    resolvedUrl = streamLink,
                    fileName = filename
                  )
                )

              }
            } else {
              Log.d("M4UFree", "getLink to resolver: $iframe")
              linkCallback(
                StreamData(
                  iframe,
                  fileName = filename
                )
              )
            }
          }
        }
      }
    }


  }

  var list = listOf(
    "HP-UX",
    "Linux i686",
    "Linux armv7l",
    "Mac68K",
    "MacPPC",
    "MacIntel",
    "SunOS",
    "Win16",
    "Win32",
    "WinCE",
    "iPhone",
    "iPod",
    "iPad",
    "Android",
    "BlackBerry",
    "Opera"
  )

  fun caesarShift(str: String, amount: Int): String {
    if (amount < 0) {
      return caesarShift(str, amount + 26);
    }
    var output = "";
    for (i in 0 until str.length) {
      var c = str[i];
      if (c.isLetter()) {
        var code = str.codePointAt(i);
        if (code >= 65 && code <= 90) {
          c = (((code - 65 + amount) % 26) + 65).toChar();
        } else if (code >= 97 && code <= 122) {
          c = (((code - 97 + amount) % 26) + 97).toChar();
        }
      }
      output += c;
    }
    return output;
  };
  fun String2Hex(tmp: String): String {
    var str = "";
    for (i in 0 until tmp.length) {
      str += tmp.codePointAt(i).toString(16);
    }
    return str;
  }

  fun mahoa_data(input: String, key: String): String {
    var a = AES256Cryptor.encrypt(input, key).toString()
    var b = a.replace("U2FsdGVkX1", "")
    b = b.replace(Regex("\n"), "")
    b = b.replace(Regex("/"), "|a")
    b = b.replace(Regex("\\+"), "|b")
    b = b.replace(Regex("="), "|c")
    b = b.replace(Regex("\\|"), "-z")

    return b;
  }

  fun f0002(input: String, key: String): String {
    val input = hexToBase64(input);
    val result = AES256Cryptor.decrypt(input, key)

    return result
  }

  fun f0001(input: String, key: String): String {
    var input = AES256Cryptor.encrypt(input, key)
    if (input.isNullOrEmpty())
      return ""
    val result = base64ToHex(input)
    return result
  }

  fun hexStringToByteArray(hex: String): ByteArray {
    val len = hex.length
    val byteArray = ByteArray(len / 2)

    for (i in 0 until len step 2) {
      byteArray[i / 2] = ((hex.substring(i, i + 2).toInt(16)) and 0xFF).toByte()
    }

    return byteArray
  }

  fun hexToBase64(hex: String): String {
    val byteArray = hexStringToByteArray(hex)
    return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
  }

  fun base64ToHex(base64Str: String): String {
    val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
    return decodedBytes.joinToString("") { String.format("%02x", it) }
  }
}
