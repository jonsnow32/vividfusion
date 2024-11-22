package cloud.app.vvf.common.models

enum class ExtensionType(val feature: String) {
  DATABASE("database"), STREAM("stream"), SUBTITLE("subtitle");
}

fun ExtensionType.priorityKey() = "priority_$this"

//Database is tmdb, imdb, tvdb, trakt
//StreamSource is scrappers, realdebrid, allldebrid, ...
//Subtitle is a openSubtitle, subtitleCat ...
