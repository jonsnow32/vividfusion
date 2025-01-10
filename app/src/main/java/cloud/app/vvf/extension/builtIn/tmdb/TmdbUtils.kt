package cloud.app.vvf.extension.builtIn.tmdb

import android.annotation.SuppressLint
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.Actor
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.movie.GeneralInfo
import cloud.app.vvf.common.models.movie.Ids
import cloud.app.vvf.common.models.movie.Movie
import cloud.app.vvf.common.models.movie.Season
import cloud.app.vvf.common.models.movie.Show
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.BasePerson
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.tmdb2.entities.PersonResultsPage
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"


fun MovieResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()
fun TvShowResultsPage.toMediaItemsList() = results?.map { it.toMediaItem() } ?: emptyList()
fun PersonResultsPage.toMediaItemsList() = results?.mapNotNull { it.toMediaItem() } ?: emptyList()

fun BaseMovie.toMediaItem(): AVPMediaItem.MovieItem {

  fun getGenre(baseMovie: BaseMovie): List<String>? {
    if (baseMovie.genres.isNullOrEmpty()) {
      return baseMovie.genre_ids?.map { movieGenres[it] ?: "" }
    }
    return baseMovie.genres?.map { it.name }
  }

  fun toMovie(baseMovie: BaseMovie) = Movie(
    ids = Ids(tmdbId = baseMovie.id),
    generalInfo = GeneralInfo(
      title = baseMovie.title ?: "No title",
      originalTitle = baseMovie.original_title ?: "No title",
      overview = baseMovie.overview,
      releaseDateMsUTC = baseMovie.release_date?.time ?: 0,
      poster = baseMovie.poster_path,
      backdrop = baseMovie.backdrop_path,
      rating = baseMovie.vote_average,
      genres = getGenre(this),
      homepage = null
      //to be continued
    )
  )

  val movie = toMovie(this);
  if (this is com.uwetrottmann.tmdb2.entities.Movie) {
    movie.ids.imdbId = this.external_ids?.imdb_id

    movie.recommendations = this.recommendations?.results?.map { toMovie(it) }
    movie.generalInfo.runtime = this.runtime
    movie.generalInfo.homepage = this.homepage
    movie.generalInfo.actors = this.credits?.cast?.map {
      Actor(name = it.name ?: "No name", image = it.profile_path?.toImageHolder(), id = it.id)
    }
  }

  return AVPMediaItem.MovieItem(movie)
}

fun BaseTvShow.toMediaItem(): AVPMediaItem.ShowItem {
  fun toShow(baseShow: BaseTvShow) = Show(
    ids = Ids(tmdbId = baseShow.id),
    generalInfo = GeneralInfo(
      title = baseShow.name ?: "No title",
      originalTitle = baseShow.original_name ?: "No title",
      overview = baseShow.overview,
      releaseDateMsUTC = baseShow.first_air_date?.time ?: 0,
      poster = baseShow.poster_path,
      backdrop = baseShow.backdrop_path,
      rating = baseShow.vote_average,
      genres = baseShow.genre_ids?.map { showGenres[it] ?: "" },
      homepage = null
      //to be continued
    ),
  )

  val show = toShow(this)
  if (this is com.uwetrottmann.tmdb2.entities.TvShow) {
    show.ids.tvdbId = this.external_ids?.tvdb_id
    show.ids.imdbId = this.external_ids?.imdb_id
    show.recommendations = this.recommendations?.results?.map { toShow(it) }
    show.generalInfo.contentRating = this.content_ratings?.results.orEmpty().firstOrNull()?.rating
    show.generalInfo.genres = this.genres?.map { it.name ?: "unknown" }
    show.status = this.status?.toString() ?: "";
    show.generalInfo.actors = this.credits?.cast?.map {
      Actor(name = it.name ?: "No name", image = it.profile_path?.toImageHolder(), id = it.id)
    }
    show.generalInfo.homepage = this.homepage
    show.seasons = this.seasons?.map { tvSeason ->
      Season(
        title = tvSeason.name,
        number = tvSeason.season_number ?: 0,
        overview = tvSeason.overview,
        episodeCount = tvSeason.episode_count ?: 0,
        posterPath = tvSeason.poster_path,
        showIds = show.ids,
        showOriginTitle = show.generalInfo.originalTitle,
        backdrop = show.generalInfo.backdrop,
        releaseDateMsUTC = tvSeason.air_date?.time
      )
    }
    show.tagLine = this.tagline
  }
  return AVPMediaItem.ShowItem(show)
}

fun BasePerson.toMediaItem() = AVPMediaItem.ActorItem(
  Actor(name = name, image = profile_path?.toImageHolder(), id = id)
)

@SuppressLint("DefaultLocale")
fun formatSeasonEpisode(season: Int, episode: Int): String {
  return String.format("S%02dE%02d", season, episode)
}

fun String.iso8601ToMillis(): Long {
  if (this.isBlank()) return 0L
  // Parse the date string using LocalDate
  val localDate = LocalDate.parse(this, DateTimeFormatter.ISO_DATE)

  // Convert the LocalDate to milliseconds since epoch (UTC)
  return localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}


val networks: Map<Int, String> = linkedMapOf( // Use LinkedHashMap to preserve order
  213 to "Netflix", // Most popular first
  1024 to "Amazon",
  4 to "CBS",
  19 to "FOX",
  54 to "NBC",
  2 to "ABC",
  34 to "HBO",
  6 to "The CW",
  30 to "FX",
  1313 to "Showtime" // Least popular last
)

val companies: Map<Int, String> = linkedMapOf( // Use LinkedHashMap to preserve order
  420 to "Marvel Studios", // Most popular first
  174 to "Warner Bros. Pictures",
  33 to "Universal Pictures",
  25 to "20th Century Fox",
  5 to "Walt Disney Pictures",
  2 to "Columbia Pictures",
  4 to "Paramount",
  12 to "New Line Cinema",
  34 to "Legendary Pictures",
  6194 to "Lucasfilm" // Least popular last
)

val showGenres: Map<Int, String> = mapOf(
  10759 to "Action & Adventure",
  16 to "Animation",
  35 to "Comedy",
  80 to "Crime",
  99 to "Documentary",
  18 to "Drama",
  10751 to "Family",
  10762 to "Kids",
  9648 to "Mystery",
  10763 to "News",
  10764 to "Reality",
  10765 to "Sci-Fi & Fantasy",
  10766 to "Soap",
  10767 to "Talk",
  10768 to "War & Politics",
  37 to "Western"
)

val movieGenres: Map<Int, String> = mapOf(
  28 to "Action",
  12 to "Adventure",
  16 to "Animation",
  35 to "Comedy",
  80 to "Crime",
  99 to "Documentary",
  18 to "Drama",
  10751 to "Family",
  14 to "Fantasy",
  36 to "History",
  27 to "Horror",
  10402 to "Music",
  9648 to "Mystery",
  10749 to "Romance",
  878 to "Science Fiction",
  10770 to "TV Movie",
  53 to "Thriller",
  10752 to "War",
  37 to "Western"
)

val languageI3691Map = mapOf(
  "aa" to "Afar",
  "af" to "Afrikaans",
  "ak" to "Akan",
  "an" to "Aragonese",
  "as" to "Assamese",
  "av" to "Avaric",
  "ae" to "Avestan",
  "ay" to "Aymara",
  "az" to "Azərbaycan",
  "ba" to "Bashkir",
  "bm" to "Bamanankan",
  "bn" to "বাংলা",
  "bi" to "Bislama",
  "bo" to "Tibetan",
  "bs" to "Bosanski",
  "br" to "Breton",
  "ca" to "Català",
  "cs" to "Český",
  "ch" to "Chamorro",
  "ce" to "Chechen",
  "cu" to "Church Slavic",
  "cv" to "Chuvash",
  "kw" to "Cornish",
  "co" to "Corsican",
  "cr" to "Cree",
  "cy" to "Cymraeg",
  "da" to "Dansk",
  "de" to "Deutsch",
  "dv" to "Divehi",
  "dz" to "Dzongkha",
  "en" to "English",
  "eo" to "Esperanto",
  "et" to "Eesti",
  "eu" to "euskera",
  "fo" to "Faroese",
  "fj" to "Fijian",
  "fi" to "suomi",
  "fr" to "Français",
  "fy" to "Frisian",
  "ff" to "Fulfulde",
  "gd" to "Scottish Gaelic",
  "ga" to "Gaeilge",
  "gl" to "Galego",
  "gv" to "Manx",
  "gn" to "Guarani",
  "gu" to "Gujarati",
  "ht" to "Haitian Creole",
  "ha" to "Hausa",
  "sh" to "Serbo-Croatian",
  "hz" to "Herero",
  "ho" to "Hiri Motu",
  "hr" to "Hrvatski",
  "hu" to "Magyar",
  "ig" to "Igbo",
  "io" to "Ido",
  "ii" to "Sichuan Yi",
  "iu" to "Inuktitut",
  "ie" to "Interlingue",
  "ia" to "Interlingua",
  "id" to "Bahasa indonesia",
  "ik" to "Inupiaq",
  "is" to "Íslenska",
  "it" to "Italiano",
  "jv" to "Javanese",
  "ja" to "日本語",
  "kl" to "Kalaallisut",
  "kn" to "ಕನ್ನಡ",
  "ks" to "Kashmiri",
  "ka" to "ქართული",
  "kr" to "Kanuri",
  "kk" to "қазақ",
  "km" to "Khmer",
  "ki" to "Kikuyu",
  "rw" to "Kinyarwanda",
  "ky" to "Кыргызча",
  "kv" to "Komi",
  "kg" to "Kongo",
  "ko" to "한국어/조선말",
  "kj" to "Kuanyama",
  "ku" to "Kurdish",
  "lo" to "Lao",
  "la" to "Latin",
  "lv" to "Latviešu",
  "li" to "Limburgish",
  "ln" to "Lingala",
  "lt" to "Lietuvių",
  "lb" to "Luxembourgish",
  "lu" to "Luba-Katanga",
  "lg" to "Ganda",
  "mh" to "Marshallese",
  "ml" to "Malayalam",
  "mr" to "Marathi",
  "mg" to "Malagasy",
  "mt" to "Malti",
  "mo" to "Moldavian",
  "mn" to "Mongolian",
  "mi" to "Māori",
  "ms" to "Bahasa melayu",
  "my" to "Burmese",
  "na" to "Nauru",
  "nv" to "Navajo",
  "nr" to "Southern Ndebele",
  "nd" to "Northern Ndebele",
  "ng" to "Ndonga",
  "ne" to "Nepali",
  "nl" to "Nederlands",
  "nn" to "Nynorsk",
  "nb" to "Bokmål",
  "no" to "Norsk",
  "ny" to "Chichewa",
  "oc" to "Occitan",
  "oj" to "Ojibwe",
  "or" to "Odia",
  "om" to "Oromo",
  "os" to "Ossetian",
  "pa" to "ਪੰਜਾਬੀ",
  "pi" to "Pali",
  "pl" to "Polski",
  "pt" to "Português",
  "qu" to "Quechua",
  "rm" to "Romansh",
  "ro" to "Română",
  "rn" to "Kirundi",
  "ru" to "Pусский",
  "sg" to "Sango",
  "sa" to "Sanskrit",
  "si" to "සිංහල",
  "sk" to "Slovenčina",
  "sl" to "Slovenščina",
  "se" to "Northern Sami",
  "sm" to "Samoan",
  "sn" to "Shona",
  "sd" to "Sindhi",
  "so" to "Somali",
  "st" to "Southern Sotho",
  "es" to "Español",
  "sq" to "shqip",
  "sc" to "Sardinian",
  "ss" to "Swati",
  "su" to "Sundanese",
  "sw" to "Kiswahili",
  "sv" to "svenska",
  "ty" to "Tahitian",
  "ta" to "தமிழ்",
  "tt" to "Tatar",
  "te" to "తెలుగు",
  "tg" to "Tajik",
  "tl" to "Tagalog",
  "th" to "ภาษาไทย",
  "ti" to "Tigrinya",
  "to" to "Tonga",
  "tn" to "Tswana",
  "ts" to "Tsonga",
  "tk" to "Turkmen",
  "tr" to "Türkçe",
  "tw" to "Twi",
  "ug" to "Uyghur",
  "uk" to "Український",
  "ur" to "اردو",
  "uz" to "ozbek",
  "ve" to "Venda",
  "vi" to "Tiếng Việt",
  "vo" to "Volapük",
  "wa" to "Walloon",
  "wo" to "Wolof",
  "xh" to "Xhosa",
  "yi" to "Yiddish",
  "za" to "Zhuang",
  "zu" to "isiZulu",
  "ab" to "Abkhazian",
  "zh" to "普通话",
  "ps" to "پښتو",
  "am" to "Amharic",
  "ar" to "العربية",
  "be" to "беларуская мова",
  "bg" to "български език",
  "cn" to "广州话 / 廣州話",
  "mk" to "Macedonian",
  "ee" to "Èʋegbe",
  "el" to "ελληνικά",
  "fa" to "فارسی",
  "he" to "עִבְרִית",
  "hi" to "हिन्दी",
  "hy" to "Armenian",
  "yo" to "Èdè Yorùbá"
)

val popularCountriesIsoToEnglishName: Map<String, String> = mapOf(
  "US" to "United States",
  "GB" to "United Kingdom",
  "CA" to "Canada",
  "AU" to "Australia",
  "DE" to "Germany",
  "FR" to "France",
  "JP" to "Japan",
  "CN" to "China",
  "IN" to "India",
  "BR" to "Brazil",
  "RU" to "Russia",
  "MX" to "Mexico",
  "ID" to "Indonesia",
  "KR" to "South Korea",
  "IT" to "Italy",
  "ES" to "Spain",
  "TR" to "Turkey",
  "SA" to "Saudi Arabia",
  "IR" to "Iran",
  "PL" to "Poland",
  "CO" to "Colombia",
  "AR" to "Argentina",
  "TH" to "Thailand",
  "ZA" to "South Africa",
  "EG" to "Egypt",
  "NG" to "Nigeria",
  "PK" to "Pakistan",
  "UA" to "Ukraine",
  "VN" to "Vietnam",
  "DZ" to "Algeria",
  "MY" to "Malaysia",
  "PH" to "Philippines",
  "ET" to "Ethiopia",
  "KE" to "Kenya",
  "IQ" to "Iraq",
  "AF" to "Afghanistan",
  "PE" to "Peru",
  "VE" to "Venezuela",
  "CL" to "Chile",
  "SD" to "Sudan",
  "SY" to "Syria",
  "MM" to "Myanmar",
  "NZ" to "New Zealand",
  "SG" to "Singapore",
  "HK" to "Hong Kong",
  "TW" to "Taiwan",
  "AE" to "United Arab Emirates"
)
