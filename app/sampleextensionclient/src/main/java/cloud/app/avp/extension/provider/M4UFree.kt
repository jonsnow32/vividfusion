package cloud.app.avp.extension.provider

import android.util.Log
import cloud.app.avp.extension.Constants
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.helpers.network.utils.AES256Cryptor
import cloud.app.common.helpers.network.utils.GoogleReCaptchar
import cloud.app.common.helpers.network.utils.MD5Utils
import cloud.app.common.helpers.network.Utils
import cloud.app.common.models.stream.StreamData
import com.domain.usecases.stream.Utils.RegexUtils
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Element

class M4UFree(private val httpHelper: HttpHelper) {
    val BASE_URL: String get() = "https://ww2.m4ufree.com"
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

        val keyword: String = Utils.replaceAllkeywithTarget(name.lowercase(), "-")
        var url = "";

        var searchUrl = String.format("%s/search/%s.html", BASE_URL, keyword);
        var doc = httpHelper.get(searchUrl).document;
        val iterator: Iterator<*> = doc.select("div.row div.item").select("a[href]").iterator()
        while (iterator.hasNext()) {
            val elsA = iterator.next() as Element;
            url = elsA!!.attr("href")
            val title = elsA.attr("title")
            if (Utils.replaceAllkeywithTarget(title.lowercase(), "").startsWith(
                    Utils.replaceAllkeywithTarget(
                        name.lowercase() + valueOf.lowercase(),
                        ""
                    )
                )
            ) {
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
    ) = flow<List<StreamData>> {
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
                json = "m4u=" + Utils.URLEncoder(playdata) + "&_token=" + token,
                    headers = hashMap2
            ).text
            Log.d("M4UFree", "getLink: $html")
            if (!html.isNullOrEmpty()) {
                var iframe =
                    RegexUtils.getGroup(html, "<iframe[^>]+src=['\"]([^'\"]+)['\"][^>]*>", 1);
                if (!iframe.isNullOrEmpty()) {
                    if (iframe.startsWith("//"))
                        iframe = "https:" + iframe
                    html = httpHelper.get(iframe,  mapOf("referer" to iframe)).text;
                    if (!html.isNullOrEmpty()) {
                        val idfile: String =
                            RegexUtils.getGroup(html, "idfile\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]", 1)
                        val idUser: String =
                            RegexUtils.getGroup(html, "idUser\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]", 1)
                        val NameKeyV3: String =
                            RegexUtils.getGroup(html, "NameKeyV3\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]", 1)
                        var DOMAIN_API: String =
                            RegexUtils.getGroup(
                                html,
                                "DOMAIN_API\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                                1
                            )
                        if(DOMAIN_API.startsWith("//"))
                            DOMAIN_API = "https:" + DOMAIN_API
                        if (!DOMAIN_API.endsWith("/"))
                            DOMAIN_API += "/"
                        val hashMap1: HashMap<String, String> = HashMap()
                        hashMap1["origin"] = Utils.getBaseUrl(iframe)
                        if(NameKeyV3.isNullOrEmpty()) {
                            html = httpHelper.post(
                                "$DOMAIN_API$idUser/$idfile",
                                data = mapOf("referrer" to BASE_URL),headers = hashMap1
                            ).text
                        } else {
                            var plf = "Win32";// list.random()
                            var domain_ref = BASE_URL;
                            val keyenc: String = RegexUtils.getGroup(
                                html,
                                "CryptoJS.MD5\\(['\"]([^'\"]+)['\"].*\\,\\s*(\\d+)\\)",
                                1
                            )
                            val mountenc: String = RegexUtils.getGroup(
                                html,
                                "CryptoJS.MD5\\(['\"]([^'\"]+)['\"].*\\,\\s*(\\d+)\\)",
                                2
                            )
                            var mahoa_data = mahoa_data(plf + '|' + idUser + '|' + idfile + '|' + domain_ref,
                                MD5Utils.StringtoHex(keyenc).toString())
                            var caesarShift = caesarShift(mahoa_data,mountenc.toInt());
                            var string2Hex = String2Hex(caesarShift);
                            val gooleCaptcharKey: String =
                                RegexUtils.getGroup(html, "captcha\\.execute\\(['\"]([^'\"]+)['\"]", 1)
                            val token: String =
                                GoogleReCaptchar.getCaptchaToken(httpHelper,iframe, gooleCaptcharKey, BASE_URL,Constants.USER_AGENT)

                            html = httpHelper.post(
                                "$DOMAIN_API",
                                data = mapOf("namekey" to NameKeyV3,"token" to token, "referrer" to BASE_URL,"data" to string2Hex+ "|" + MD5Utils.StringtoHex(string2Hex + "plhq@@@22").toString()), headers =  hashMap1
                            ).text
                            if(html.isNullOrEmpty()){}
                        }
                        iframe =
                            RegexUtils.getGroup(
                                html!!,
                                "data['\"]\\s*:\\s*['\"]([^'\"]+[^'\"])['\"]",
                                1
                            )
                        if (!iframe.isNullOrEmpty()) {
                            Log.d("M4UFree", "getLink: $iframe")
                         emit(
                             listOf(
                                 StreamData(
                                     iframe,
                                     fileName = filename
                                 )
                             )
                         )

//                            val clone = StreamEntity(iframe)
//                            val ishsl = iframe.contains(Regex("(?:m3u8|hls)"))
//                            if (ishsl) {
//                                clone.isCDN = true
//                                clone.resolverName = "CDN"
//                                playhaeder.put("origin", UrlUtils.getBaseUrl(iframe))
//                                clone.headers = playhaeder
//                            }
//
//                            clone.quality = Quality.convert(matchQuality)
//                            clone.fileName = filename
//                            clone.premiumType = 0
//                            clone.providerName = name
//                            producerScope.send(clone)
                        }
                    }
                }
            }
        }


    }

    var list = listOf("HP-UX",
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
        "Opera")
    fun caesarShift (str : String, amount : Int) : String {
        if (amount < 0) {
            return caesarShift(str, amount + 26);
        }
        var output = "";
        for (i in 0 until str.length) {
            var c = str[i];
            if (c.isLetter()) {
                var code = str.codePointAt(i);
                if (code >= 65 && code <= 90) {
                    c =(((code - 65 + amount) % 26) + 65).toChar();
                }
                else if (code >= 97 && code <= 122) {
                    c = (((code - 97 + amount) % 26) + 97).toChar();
                }
            }
            output += c;
        }
        return output;
    };
    fun String2Hex(tmp:String) :String{
        var str = "";
        for (i in 0 until tmp.length) {
            str += tmp.codePointAt(i).toString(16);
        }
        return str;
    }
    fun mahoa_data(input :String, key :String) :String {
        var a = AES256Cryptor.encrypt(input, key).toString();
        var b = a.replace("U2FsdGVkX1", "");
        b = b.replace(Regex("\n"), "");
        b = b.replace(Regex("\\/"), "|a")
        b = b.replace(Regex("\\+"), "|b");
        b = b.replace(Regex("\\="), "|c");
        b = b.replace(Regex("\\|"), "-z");

        return b;
    }
}