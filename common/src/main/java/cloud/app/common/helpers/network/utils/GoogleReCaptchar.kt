package cloud.app.common.helpers.network.utils

import android.net.Uri
import android.util.Base64
import cloud.app.common.helpers.network.HttpHelper
import com.domain.usecases.stream.Utils.RegexUtils.getGroup

object GoogleReCaptchar {
    suspend fun getCaptchaToken(
        httpHelper: HttpHelper,
        url: String?,
        key: String,
        referer: String?,
        userAgent: String
    ): String {
        var referer = referer
        val uri = Uri.parse(url)
        val base_domain = uri.scheme + "://" + uri.host
        val domain = Base64.encodeToString(
            "$base_domain:443".toByteArray(),
            0
        ).replace("\n", "").replace("=", ".")

        referer = "https://www.google.com/recaptcha/api.js?render=$key"
        var html: String = httpHelper.get(referer,headers = mapOf("referer" to referer)).text
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
            val data =
                String.format("v=%s&k=%s&c=%s&co=%s&reason=q", vtoken, key, recapToken, domain)
            html = httpHelper.post(
                "https://www.google.com/recaptcha/api2/reload?k=$key",
                json =  data,
                headers = hashMap
            ).text
            if (!html.isEmpty()) {
                val token = getGroup(html, "rresp['\"]\\,['\"]([^'\"]+)['\"]", 1)
                return token
            }
        }

        return ""
    }
}