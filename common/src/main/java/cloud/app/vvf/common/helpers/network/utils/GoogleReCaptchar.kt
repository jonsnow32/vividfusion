package cloud.app.vvf.common.helpers.network.utils

import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.helpers.network.utils.RegexUtils.getGroup
import java.net.URI
import java.util.Base64

object GoogleReCaptcha {
  suspend fun getCaptchaToken(
    httpHelper: HttpHelper,
    url: String?,
    key: String,
    referer: String?,
    userAgent: String
  ): String {
    var referer = referer
    val uri = URI(url)
    val baseDomain = uri.scheme + "://" + uri.host
    val domain = Base64.getEncoder()
      .encodeToString("$baseDomain:443".toByteArray())
      .replace("\n", "")
      .replace("=", ".")

    referer = "https://www.google.com/recaptcha/api.js?render=$key"
    var html: String = httpHelper.get(referer, headers = mapOf("referer" to referer)).text
    val vtoken = getGroup(html, "releases\\/([0-9a-zA-Z\\.:_\\&\\#\\*\\%\\~\\^\\-\\!]+)", 1)

    referer = String.format(
      "https://www.google.com/recaptcha/api2/anchor?ar=1&hl=en&size=invisible&cb=cs3&k=%s&co=%s&v=%s",
      key,
      domain,
      vtoken
    )
    html = httpHelper.get(referer).text

    val hashMap: HashMap<String, String> = HashMap()
    hashMap["User-Agent"] = userAgent
    val recapToken =
      getGroup(html, "<input[^>]*recaptcha-token.*value\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", 1)
    if (recapToken != null) {
      val data = mapOf("v" to vtoken, "k" to key, "c" to recapToken, "co" to domain, "reason" to "q")
      html = httpHelper.post(
        "https://www.google.com/recaptcha/api2/reload?k=$key",
        data = data,
        headers = hashMap
      ).text
      if (html.isNotEmpty()) {
        val token = getGroup(html, "rresp['\"]\\,['\"]([^'\"]+)['\"]", 1)
        return token
      }
    }

    return ""
  }
}
