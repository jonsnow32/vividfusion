package cloud.app.vvf.network.api.openSubtitle

import android.content.SharedPreferences
import cloud.app.vvf.network.api.openSubtitle.models.User
import com.google.gson.Gson
import javax.inject.Inject

class OpenSubtitleOAuthSettings @Inject constructor(
    val sharedPreferences: SharedPreferences,
    val gson: Gson
) {

    fun saveAccessToken(accessToken: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun saveUser(user: User) {
        sharedPreferences.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    fun getAccessToken(): String {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, "") ?: ""
    }

    fun getUser(): User? {
        val userJson = sharedPreferences.getString(KEY_USER, "")
        if (userJson.isNullOrEmpty())
            return null
        return gson.fromJson(userJson, User::class.java)
    }

    fun getUserStr(): String? {
        val userJson = sharedPreferences.getString(KEY_USER, "")
        if (userJson.isNullOrEmpty())
            return null
        val user = gson.fromJson(userJson, User::class.java)
        return "ID: ${user.user_id} \nLevel: ${user.level} \nUser Type: ${if (user.vip) "Vip" else "Normal"}\nAllowed Download: ${user.allowed_downloads}\nDownload count: ${user.downloads_count} \nRemaining downloads: ${user.remaining_downloads}"
    }

    fun clear() {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, "").apply()
        sharedPreferences.edit().putString(KEY_USER, "").apply()
    }

    companion object {
        const val KEY_ACCESS_TOKEN = "pref_open_subtitle_access_token"
        const val KEY_USER = "pref_open_subtitle_user"
    }
}
