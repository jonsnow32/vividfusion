package cloud.app.vvf.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import com.uwetrottmann.trakt5.TraktLink
import org.threeten.bp.chrono.IsoChronology
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.format.FormatStyle
import timber.log.Timber
import java.io.DataOutputStream
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.text.CharacterIterator
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*
import java.util.regex.Pattern


object Utils {

    private const val IMDB_APP_TITLE_URI_POSTFIX = "/"

    private const val IMDB_APP_TITLE_URI = "imdb:///title/"

    const val IMDB_TITLE_URL = "http://imdb.com/title/"

    const val TMDB_BASE_URL = "https://www.themoviedb.org/"
    const val TMDB_PATH_TV = "tv/"
    const val TMDB_PATH_MOVIES = "movie/"
    const val TMDB_PATH_PERSON = "person/"

    private const val YOUTUBE_BASE_URL = "http://www.youtube.com/watch?v="

    private const val YOUTUBE_SEARCH = "http://www.youtube.com/results?search_query=%s"

    private const val YOUTUBE_PACKAGE = "com.google.android.youtube"


    var REMIDER_UPDATE_LATER = false;

    fun launchMarketPlay(context: Context, packageName: String) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse(
                        "market://details?id=$packageName"
                    )
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse(
                        "https://play.google.com/store/apps/details?id=$packageName"
                    )
                )
            )
        }
    }

    fun launchBrowser(context: Context?, url: String?): Boolean {
        if (context == null || TextUtils.isEmpty(url)) {
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return openNewDocument(context, intent)
    }

    fun launchWebsite(context: Context, url: String) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        val builder = CustomTabsIntent.Builder()
        builder.setUrlBarHidingEnabled(false)

        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setNavigationBarColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .build()
        builder.setDefaultColorSchemeParams(colorParams)

        val customTabsIntent = builder.build()
        customTabsIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    fun openImdb(imdbId: String, context: Context?) {
        if (context == null || TextUtils.isEmpty(imdbId)) {
            return
        }

        // try launching the IMDb app
        val intent = Intent(
            Intent.ACTION_VIEW, Uri
                .parse(IMDB_APP_TITLE_URI + imdbId + IMDB_APP_TITLE_URI_POSTFIX)
        )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        if (!tryStartActivity(context, intent, false)) {
            // on failure, try launching the web page
            launchWebsite(context, imdbLink(imdbId))
        }
    }

    fun imdbLink(imdbId: String): String {
        return IMDB_TITLE_URL + imdbId
    }

    fun tmdbMovieLink(tmdbId: Int): String {
        return TMDB_BASE_URL + TMDB_PATH_MOVIES + tmdbId
    }

    fun tmdbTvLink(tmdbId: Int): String {
        return TMDB_BASE_URL + TMDB_PATH_TV + tmdbId
    }

    fun tmdbEpisodeUrl(showTmdbId: Int, season: Int, episode: Int): String {
        return "$TMDB_BASE_URL$TMDB_PATH_TV$showTmdbId/season/$season/episode/$episode"
    }

    fun tmdbPersonLink(tmdbId: Int): String {
        return TMDB_BASE_URL + TMDB_PATH_PERSON + tmdbId
    }

    fun traktShowUrl(showTmdbId: Int): String? {
        return TraktLink.tmdb(showTmdbId) + "?id_type=show"
    }

    fun traktEpisodeUrl(episodeTmdbId: Int): String? {
        return TraktLink.tmdb(episodeTmdbId) + "?id_type=episode"
    }

    fun traktEpisodetvdbUrl(episodeTmdbId: Int): String? {
        return TraktLink.tvdb(episodeTmdbId) + "?id_type=episode"
    }

    fun traktMovieUrl(movieTmdbId: Int): String? {
        return TraktLink.tmdb(movieTmdbId) + "?id_type=movie"
    }

    fun metacriticMovie(title: String): String {
        return "https://www.metacritic.com/search/movie/${Uri.encode(title)}/results"
    }

    /**
     * Starts VIEW Intent with Metacritic website TV search results URL.
     */
    fun metacriticTvShow(title: String): String {
        return "https://www.metacritic.com/search/tv/${Uri.encode(title)}/results"
    }

    fun launchYoutube(context: Context, key: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=" + key)
        )
        intent.component =
            ComponentName("com.google.android.youtube", "com.google.android.youtube.PlayerActivity")

        val manager = context.packageManager

        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        if (infos.size > 0) {
            context.startActivity(intent)
        } else {
            //No Application can handle your intent
            launchWebsite(context, "https://www.youtube.com/watch?v=" + key)
            //Toast.makeText(context,"Youtube not installed",Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Tries to start the given intent as a new document (e.g. opening a website, other app) so it
     * appears as a new entry in the task switcher using [.tryStartActivity].
     */
    fun openNewDocument(context: Context, intent: Intent): Boolean {
        // launch as a new document (separate entry in task switcher)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return Utils.tryStartActivity(context, intent, true)
    }

    @SuppressLint("LogNotTimber")
    fun tryStartActivity(context: Context, intent: Intent?, displayError: Boolean): Boolean {
        // Note: Android docs suggest to use resolveActivity,
        // but won't work on Android 11+ due to package visibility changes.
        // https://developer.android.com/about/versions/11/privacy/package-visibility
        val handled: Boolean
        handled = try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            // catch failure to handle explicit intents
            // log in release builds to help extension developers debug
            Log.i("Utils", "Failed to launch intent.", e)
            false
        } catch (e: SecurityException) {
            Log.i("Utils", "Failed to launch intent.", e)
            false
        }
        if (displayError && !handled) {
            Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show()
        }
        return handled
    }

    fun getDeviceSuperInfo(context: Context): String {
        Timber.i("getDeviceSuperInfo");
        var s = "";
        try {
            s += " Build version: " + getBuildVersion(context)
            //s += "\n VersionName: " + getBuildVersionName(context)
            s += "\n OS Version: " + Build.VERSION.RELEASE
            s += "\n API Level: " + Build.VERSION.SDK_INT
            s += "\n DEVICE: " + Build.DEVICE
            s += "\n MODEL: " + Build.MODEL
            s += "\n CPU_ABI: " + getAbi()
            s += "\n HARDWARE: " + Build.HARDWARE
            s += "\n MANUFACTURER: " + Build.MANUFACTURER
            s += "\n USER: " + Build.USER
            s += "\n HOST: " + Build.HOST
            Timber.i(" | Device Info > " + s)
        } catch (e: Exception) {
            Timber.e("Error getting Device INFO" + e.message)
        }
        return s;
    }//end getDeviceSuperInfo

    fun getAbi(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // on newer Android versions, we'll return only the most important Abi version
            Build.SUPPORTED_ABIS[0]
        } else {
            // on pre-Lollip versions, we got only one Abi
            Build.CPU_ABI
        }
    }

    fun getBuildVersion(context: Context): String? {
        val manager = context?.packageManager
        val info = manager?.getPackageInfo(
            context.packageName, 0
        )
        val versionName = info?.versionName
        val versionNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info?.longVersionCode
        } else {
            info?.versionCode
        }
        return versionNumber.toString();
    }


    fun getBuildVersionName(context: Context): String? {
        val manager = context?.packageManager
        val info = manager?.getPackageInfo(
            context.packageName, 0
        )
        return info?.versionName
    }

    fun getDefaultDatePattern(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.MEDIUM,
                null,
                IsoChronology.INSTANCE,
                Locale.getDefault()
            )
        } else {
            SimpleDateFormat().toPattern()
        }
    }

    fun setRating(
        parentView: View,
        rateText: TextView,
        rate: Double?,
        voteText: TextView,
        vote: Int?
    ) {
        rate?.let {
            if (it > 0) {
                parentView.visibility = View.VISIBLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    rateText.text = Html.fromHtml(
                        String.format("<b>%.1f</b>/10", rate),
                        Html.FROM_HTML_MODE_COMPACT
                    )
                } else {
                    rateText.text = Html.fromHtml(String.format("<b>%s</b>/10", rate))
                }
                voteText.text = vote.toString() + " votes"
            } else {
                parentView.visibility = View.GONE
            }
        } ?: run {
            parentView.visibility = View.GONE
        }
    }

    fun isPackageExist(context: Context, target: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(target, GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    }

    fun humanReadableByteCountBin(bytes: Long): String {
        val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(bytes)
        if (absB < 1024) {
            return "$bytes B"
        }
        var value = absB
        val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= java.lang.Long.signum(bytes).toLong()
        return java.lang.String.format("%.1f %cB", value / 1024.0, ci.current())
    }

    private val NONLATIN: Pattern = Pattern.compile("[^\\w-]")
    private val WHITESPACE: Pattern = Pattern.compile("[\\s]")

    fun toSlug(input: String): String {
        val nowhitespace: String = WHITESPACE.matcher(input).replaceAll("-")
        val normalized: String = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
        val slug: String = NONLATIN.matcher(normalized).replaceAll("")
        return slug.lowercase(Locale.ENGLISH)
    }

    fun getDomainName(url: String): String {
        return try {
            val uri = URI(url)
            var domain = uri.host
            val pos = domain.lastIndexOf(".")
            if (pos > -1) domain = domain.substring(0, pos)
            if (domain.startsWith("www.")) domain.substring(4) else domain
        } catch (e: URISyntaxException) {
            "UNKNOW HOST"
        }
    }

    fun hideDefaultControls(activity: Activity) {
        val window = activity.window ?: return
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val decorView = window.decorView
        if (decorView != null) {
            var uiOptions = decorView.systemUiVisibility
            if (Build.VERSION.SDK_INT >= 14) {
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LOW_PROFILE
            }
            if (Build.VERSION.SDK_INT >= 16) {
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
            if (Build.VERSION.SDK_INT >= 19) {
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            decorView.systemUiVisibility = uiOptions
        }
    }

    fun showDefaultControls(activity: Activity) {
        val window = activity.window ?: return
        //
//        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        val decorView = window.decorView
        if (decorView != null) {
            var uiOptions = decorView.systemUiVisibility
            if (Build.VERSION.SDK_INT >= 14) {
                uiOptions = uiOptions and View.SYSTEM_UI_FLAG_LOW_PROFILE.inv()
            }
            if (Build.VERSION.SDK_INT >= 16) {
                uiOptions = uiOptions and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
            }

//            if (Build.VERSION.SDK_INT >= 19) {
//                uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//            }
            decorView.systemUiVisibility = uiOptions
        }
    }

    fun isAndroidTV(context: Context): Boolean {
        val isTelevision = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        return isTelevision
    }

    @JvmStatic
    fun isDeviceRooted(): Boolean {
        // Check for su binary
        val su = File("/system/xbin/su")
        if (su.exists()) {
            return true
        }

        // Check for other binaries
        val paths = arrayOf(
            "/sbin/su", "/system/bin/su", "/system/sbin/su", "/system/xbin/sudo",
            "/system/bin/failsafe/su", "/data/local/su", "/data/local/xbin/su",
            "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/.ext/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }

        // Check for write permissions
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n")
            os.flush()

            if (process.waitFor() == 0) {
                return true
            }
        } catch (e: Exception) {
            // Do nothing
        }

        return false
    }

    fun hasAllFilesAccessPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // For Android 10 and below, check if WRITE_EXTERNAL_STORAGE permission is granted
            (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    fun requestAllFilesAccessPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                val uri: Uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                activity.startActivityForResult(intent, requestCode)
            } catch (e: Exception) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        requestCode
                    )
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6 (Marshmallow) to Android 10, use WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            )
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    requestCode
                )
        }
    }

    fun getSubtitleFile(fileName: String): File {
        val downloadPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
        // Get the directory for subtitle files
        val subtitlesDir = File(File(downloadPath), "subtitles")

        // If the directory does not exist, create it
        if (!subtitlesDir.exists()) {
            subtitlesDir.mkdirs() // Create the directory including any necessary but nonexistent parent directories
        }

        // Create the file object for the subtitle file
        return File(subtitlesDir, fileName)
    }

    fun getBackUpFolder(): File {
        val backupPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath + "/sstream/backup/"
        val backupFolder = File(backupPath)
        if (!backupFolder.exists()) {
            backupFolder.mkdirs() // Create the directory including any necessary but nonexistent parent directories
        }
        return backupFolder
    }

    fun convertToMineType(fileName: String): String? {
        var subtitleMimeType: String? = null
        val fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1)
        if (fileExtension.contains("srt")) {
            return "application/x-subrip";
        } else if (fileExtension.contains("webvtt"))
            return "text/vtt"
        else if (fileExtension.contains("sub"))
            return "text/x-ssa"
        else if (fileExtension.contains("ass"))
            return "text/plain"
        return subtitleMimeType
    }

    fun <T> List<T>.getItemPositionByName(item: T): Int {
        this.forEachIndexed { index, it ->
            if (it == item)
                return index
        }
        return 0
    }

  fun hideKeyboard(view: View?) {
    if (view == null) return

    val inputMethodManager =
      view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
  }

  fun showInputMethod(view: View?) {
    if (view == null) return
    val inputMethodManager =
      view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    inputMethodManager?.showSoftInput(view, 0)
  }

  fun getScreenOrientation(context: Context): String {
    return when (context.resources.configuration.orientation) {
      Configuration.ORIENTATION_LANDSCAPE -> "LANDSCAPE"
      Configuration.ORIENTATION_PORTRAIT -> "PORTRAIT"
      Configuration.ORIENTATION_UNDEFINED -> "UNDEFINED"
      else -> "UNKNOWN"
    }
  }
  fun Context.getEpisodeShortTitle(episodeItem: AVPMediaItem.EpisodeItem?): String {
    episodeItem ?: return ""
    return getString(R.string.episode_short_format, episodeItem.seasonItem.season.number, episodeItem.episode.episodeNumber)
  }
}
