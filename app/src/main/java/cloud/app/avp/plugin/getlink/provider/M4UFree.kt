package cloud.app.avp.extension.provider

import android.util.Log
import cloud.app.avp.extension.Constants
import cloud.app.avp.plugin.getlink.BaseScaper
import cloud.app.avp.utils.TimeUtils

import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.helpers.network.Utils
import cloud.app.common.helpers.network.utils.AES256Cryptor
import cloud.app.common.helpers.network.utils.GoogleReCaptchar
import cloud.app.common.helpers.network.utils.MD5Utils
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.Movie
import cloud.app.common.models.stream.StreamData
import com.domain.usecases.stream.Utils.RegexUtils
import kotlinx.coroutines.channels.ProducerScope
import org.jsoup.nodes.Element

class M4UFree ( val httpHelper: HttpHelper) : BaseScaper() {
    val BASE_URL: String get() = "https://m4ufree.se"

    override val name: String
        get() = "M4UFree"

    override suspend fun getMovieStream(movie: Movie, producerScope: ProducerScope<List<StreamData>>) {
        val url = search(movie.generalInfo.title, TimeUtils.getYear(movie.generalInfo.releaseDateMsUTC!!).toString(), -1, -1)
        if(url != null) {
            getLink(url, -1, -1, movie.generalInfo.title,producerScope)
        }

    }

    override suspend fun getEpisodeStream(episode: Episode, producerScope: ProducerScope<List<StreamData>>) {

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
        producerScope: ProducerScope<List<StreamData>>
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
                                    "idfile\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                                    1
                                )
                            val idUser: String =
                                RegexUtils.getGroup(
                                    html,
                                    "idUser\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                                    1
                                )
                            val NameKeyV3: String =
                                RegexUtils.getGroup(
                                    html,
                                    "NameKeyV3\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                                    1
                                )
                            var DOMAIN_API: String =
                                RegexUtils.getGroup(
                                    html,
                                    "DOMAIN_API\\s*=\\s*['\"]([^'\"]+[^'\"])['\"]",
                                    1
                                )
                            if (DOMAIN_API.startsWith("//"))
                                DOMAIN_API = "https:" + DOMAIN_API
                            if (!DOMAIN_API.endsWith("/"))
                                DOMAIN_API += "/"
                            val hashMap1: HashMap<String, String> = HashMap()
                            hashMap1["origin"] = Utils.getBaseUrl(iframe)
                            if (NameKeyV3.isNotEmpty() && idfile.isNotEmpty() && idUser.isNotEmpty()) {
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
                                var mahoa_data = mahoa_data(
                                    plf + '|' + idUser + '|' + idfile + '|' + domain_ref,
                                    MD5Utils.StringtoHex(keyenc).toString()
                                )
                                var caesarShift = caesarShift(mahoa_data, mountenc.toInt());
                                var string2Hex = String2Hex(caesarShift);
                                val gooleCaptcharKey: String =
                                    RegexUtils.getGroup(
                                        html,
                                        "captcha\\.execute\\(['\"]([^'\"]+)['\"]",
                                        1
                                    )
                                val ggtoken: String =
                                    GoogleReCaptchar.getCaptchaToken(
                                        httpHelper,
                                        iframe,
                                        gooleCaptcharKey,
                                        newUrl,
                                        Constants.USER_AGENT
                                    )

                                html = httpHelper.post(
                                    "$DOMAIN_API",
                                    data = mapOf(
                                        "namekey" to NameKeyV3,
                                        "token" to ggtoken,
                                        "referrer" to "$BASE_URL/",
                                        "data" to (string2Hex + "|" + MD5Utils.StringtoHex(
                                            string2Hex + "plhq@@@22"
                                        )
                                            .toString())
                                    ),
                                    headers = hashMap1
                                ).text
                                if (html.isNullOrEmpty()) {
                                }
                            }
                            var streamLink =
                                RegexUtils.getGroup(
                                    html!!,
                                    "data['\"]\\s*:\\s*['\"]([^'\"]+[^'\"])['\"]",
                                    1
                                )
                            if (!streamLink.isNullOrEmpty()) {
                                producerScope.send(
                                    listOf(
                                        StreamData(
                                            iframe,
                                            resolvedUrl = streamLink,
                                            fileName = filename
                                        )
                                    )

                                )

                            }
                        } else {
                            Log.d("M4UFree", "getLink to resolver: $iframe")
                            producerScope.send(
                                listOf(
                                    StreamData(
                                        iframe,
                                        fileName = filename
                                    )
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