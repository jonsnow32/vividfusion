package cloud.app.vvf.ui.setting

import android.content.Context
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import cloud.app.vvf.R
import cloud.app.vvf.VVFApplication.Companion.applyUiChanges
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.utils.MaterialListPreference
import cloud.app.vvf.utils.SubtitleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlin.math.max

// Change local language settings in the app.
fun getCurrentLocale(context: Context): String {
  val res = context.resources
  val conf = res.configuration
  return conf?.locales?.get(0)?.language ?: "en"
}

// idk, if you find a way of automating this it would be great
// https://www.iemoji.com/view/emoji/1794/flags/antarctica
// Emoji Character Encoding Data --> C/C++/Java Src
// https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes leave blank for auto
val appLanguages = arrayListOf(
  /* begin language list */
  Triple("", "Afrikaans", "af"),
  Triple("", "عربي شامي", "ajp"),
  Triple("", "አማርኛ", "am"),
  Triple("", "العربية", "ar"),
  Triple("", "اللهجة النجدية", "ars"),
  Triple("", "অসমীয়া", "as"),
  Triple("", "български", "bg"),
  Triple("", "বাংলা", "bn"),
  Triple("\uD83C\uDDE7\uD83C\uDDF7", "português brasileiro", "bp"),
  Triple("", "čeština", "cs"),
  Triple("", "Deutsch", "de"),
  Triple("", "Ελληνικά", "el"),
  Triple("", "English", "en"),
  Triple("", "Esperanto", "eo"),
  Triple("", "español", "es"),
  Triple("", "فارسی", "fa"),
  Triple("", "fil", "fil"),
  Triple("", "français", "fr"),
  Triple("", "galego", "gl"),
  Triple("", "हिन्दी", "hi"),
  Triple("", "hrvatski", "hr"),
  Triple("", "magyar", "hu"),
  Triple("\uD83C\uDDEE\uD83C\uDDE9", "Bahasa Indonesia", "in"),
  Triple("", "italiano", "it"),
  Triple("\uD83C\uDDEE\uD83C\uDDF1", "עברית", "iw"),
  Triple("", "日本語 (にほんご)", "ja"),
  Triple("", "ಕನ್ನಡ", "kn"),
  Triple("", "한국어", "ko"),
  Triple("", "lietuvių kalba", "lt"),
  Triple("", "latviešu valoda", "lv"),
  Triple("", "македонски", "mk"),
  Triple("", "മലയാളം", "ml"),
  Triple("", "bahasa Melayu", "ms"),
  Triple("", "Malti", "mt"),
  Triple("", "ဗမာစာ", "my"),
  Triple("", "नेपाली", "ne"),
  Triple("", "Nederlands", "nl"),
  Triple("", "norsk nynorsk", "nn"),
  Triple("", "norsk bokmål", "no"),
  Triple("", "ଓଡ଼ିଆ", "or"),
  Triple("", "polski", "pl"),
  Triple("\uD83C\uDDF5\uD83C\uDDF9", "português", "pt"),
  Triple("\uD83E\uDD8D", "mmmm... monke", "qt"),
  Triple("", "română", "ro"),
  Triple("", "русский", "ru"),
  Triple("", "slovenčina", "sk"),
  Triple("", "Soomaaliga", "so"),
  Triple("", "svenska", "sv"),
  Triple("", "தமிழ்", "ta"),
  Triple("", "ትግርኛ", "ti"),
  Triple("", "Tagalog", "tl"),
  Triple("", "Türkçe", "tr"),
  Triple("", "українська", "uk"),
  Triple("", "اردو", "ur"),
  Triple("", "Tiếng Việt", "vi"),
  Triple("", "中文", "zh"),
  Triple("\uD83C\uDDF9\uD83C\uDDFC", "正體中文(臺灣)", "zh-rTW"),
  /* end language list */
).sortedBy { it.second.lowercase() } //ye, we go alphabetical, so ppl don't put their lang on top



class GeneralSettingsFragment : BaseSettingsFragment() {
  override val title get() = getString(R.string.general_setting)
  override val transitionName = "general_setting"
  override val container  =  { GeneralPreference() }

  class GeneralPreference : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      val context = preferenceManager.context
      preferenceManager.sharedPreferencesName = context.packageName
      preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
      val screen = preferenceManager.createPreferenceScreen(context)
      val preferences = preferenceManager.sharedPreferences ?: return
      preferenceScreen = screen
      fun uiListener(block: (Any) -> Unit = {}) =
        Preference.OnPreferenceChangeListener { pref, new ->
          block(new)
          true
        }

      MaterialListPreference(context).apply {
        val tempLangs = appLanguages.toMutableList()
        val current = getCurrentLocale(context)
        val languageCodes = tempLangs.map { (_, _, iso) -> iso }
        val languageNames = tempLangs.map { (emoji, name, iso) ->
          val flag = emoji.ifBlank { SubtitleHelper.getFlagFromIso(iso) ?: "ERROR" }
          "$flag $name"
        }
        val index = max(languageCodes.indexOf(current), 0)

        key = getString(R.string.pref_locale)
        title = getString(R.string.app_language)
        summary = preferences.getString(key, languageNames[index])
        layoutResource = R.layout.preference
        isIconSpaceReserved = false

        entries = languageNames.toTypedArray()
        entryValues = languageCodes.toTypedArray()
        value = preferences.getString(key, languageNames[index])
        onPreferenceChangeListener = uiListener {
          activity?.application?.applyUiChanges(
            preferences,
            it as String,
            currentActivity = activity
          )
        }
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_public_24)
        screen.addPreference(this)
      }

      SwitchPreferenceCompat(context).apply {
        layoutResource = R.layout.preference_switch
        key = getString(R.string.pref_show_exit_confirm)
        title = getString(R.string.exit_confirm)
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_logout)
        screen.addPreference(this)
      }
    }
  }
}
