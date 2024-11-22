//package cloud.app.avp.utils
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.net.Uri
//import android.os.Build
//import androidx.annotation.WorkerThread
//import androidx.tvprovider.media.tv.PreviewChannelHelper
//import androidx.tvprovider.media.tv.TvContractCompat
//import cloud.app.avp.R
//import cloud.app.vvf.common.models.AVPMediaItem
//import com.google.android.mediahome.video.WatchNextProgram
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import timber.log.Timber
//
//const val APP_WATCH_NEXT = "avpwatchnext"
//
//fun Context.getFullName(name: String?, episode: Int?, season: Int?): String {
//  val rEpisode = episode.takeIf { it != 0 }
//  val rSeason = season.takeIf { it != 0 }
//
//  val seasonName = getString(R.string.season)
//  val episodeName = getString(R.string.episode)
//  val seasonNameShort = getString(R.string.season_short)
//  val episodeNameShort = getString(R.string.episode_short)
//
//  return when {
//    name != null && rSeason != null && rEpisode != null -> "$seasonNameShort$rSeason:$episodeNameShort$rEpisode $name"
//    name != null && rEpisode != null -> "$episodeName $rEpisode. $name"
//    name != null -> name
//    rSeason != null && rEpisode != null -> "$seasonName $rSeason - $episodeName $rEpisode"
//    rEpisode != null -> "$episodeName $rEpisode"
//    else -> ""
//  }
//}
//
//@SuppressLint("RestrictedApi")
//fun Context.buildEpisodeWatchNextProgram(
//  episode: AVPMediaItem.EpisodeItem,
//): WatchNextProgram {
//  val title =
//    getFullName(episode.title, episode.episode.episodeNumber, episode.episode.seasonNumber)
//
//  val builder = WatchNextProgram.Builder().setEpisodeTitle(title)
//    .setType(TvContractCompat.WatchNextPrograms.TYPE_TV_EPISODE)
//    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE).setTitle(title)
//    .setPosterArtUri(
//      Uri.parse(
//        episode.show?.generalInfo?.poster ?: episode.episode.generalInfo.poster ?: ""
//      )
//    ).setIntentUri(Uri.parse("$APP_WATCH_NEXT://${episode.id}"))
//    .setLastEngagementTimeUtcMillis(
//      episode.episode.updateTime
//    )
//
//  episode.episode.generalInfo.runtime?.let {
//    builder.setDurationMillis(it * 60 * 1000L)
//  }
//  builder.setLastPlaybackPositionMillis(episode.episode.position.toInt())
//  builder.setEpisodeNumber(episode.episode.episodeNumber)
//
//  return builder.build()
//}
//
//@SuppressLint("RestrictedApi")
//fun Context.buildMovieWatchNextProgram(
//  movieItem: AVPMediaItem.MovieItem
//): WatchNextProgram {
//  val title = movieItem.title
//
//  val builder = WatchNextProgram.Builder().setEpisodeTitle(title)
//    .setType(TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
//    .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE).setTitle(title)
//    .setPosterArtUri(
//      Uri.parse(
//        movieItem.movie.generalInfo.poster
//      )
//    ).setIntentUri(Uri.parse("$APP_WATCH_NEXT://${movieItem.id}"))
//    .setLastEngagementTimeUtcMillis(
//      movieItem.movie.updateTime
//    )
//
//  movieItem.movie.generalInfo.runtime?.let {
//    builder.setDurationMillis(it * 60 * 1000L)
//  }
//  builder.setLastPlaybackPositionMillis(movieItem.movie.position.toInt())
//  return builder.build()
//}
//
//// https://github.com/googlearchive/leanback-homescreen-channels/blob/master/app/src/main/java/com/google/android/tvhomescreenchannels/SampleTvProvider.java
//private val continueWatchingLock = Mutex()
//
//@SuppressLint("RestrictedApi")
//@Throws
//@WorkerThread
//suspend fun Context.addProgramsToContinueWatching(data: List<AVPMediaItem>) {
//  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
//  val context = this
//  continueWatchingLock.withLock {
//    //A way to get all last watched timestamps
//    val timeStampHashMap = HashMap<Int, AVPMediaItem>()
//    getAllResumeStateIds()?.forEach { id ->
//      val lastWatched = getLastWatched(id) ?: return@forEach
//      timeStampHashMap[lastWatched.parentId] = lastWatched
//    }
//
//    val currentProgramIds = data.mapNotNull { episodeInfo ->
//      try {
//        val customId = "${episodeInfo.id}|${episodeInfo.apiName}|${episodeInfo.url}"
//        val (program, id) = getWatchNextProgramByVideoId(customId, context)
//        val nextProgram = buildWatchNextProgramUri(
//          context,
//          episodeInfo,
//          timeStampHashMap[episodeInfo.id]
//        )
//
//        // If the program is already in the Watch Next row, update it
//        if (program != null && id != null) {
//          PreviewChannelHelper(context).updateWatchNextProgram(
//            nextProgram,
//            id,
//          )
//          id
//        } else {
//          PreviewChannelHelper(context)
//            .publishWatchNextProgram(nextProgram)
//        }
//      } catch (e: Exception) {
//        Timber.e(e)
//        null
//      }
//    }.toSet()
//
//    val allOldPrograms = getAllWatchNextPrograms(context) - currentProgramIds
//
//    // Ensures synced watch next progress by deleting all old programs.
//    allOldPrograms.forEach {
//      context.contentResolver.delete(
//        TvContractCompat.buildWatchNextProgramUri(it),
//        null, null
//      )
//    }
//  }
//}
//
//@SuppressLint("RestrictedApi")
//fun getAllWatchNextPrograms(context: Context): Set<Long> {
//  val COLUMN_WATCH_NEXT_ID_INDEX = 0
//  val cursor = context.contentResolver.query(
//    TvContractCompat.WatchNextPrograms.CONTENT_URI,
//    androidx.tvprovider.media.tv.WatchNextProgram.PROJECTION,
//    /* selection = */ null,
//    /* selectionArgs = */ null,
//    /* sortOrder = */ null
//  )
//  val set = mutableSetOf<Long>()
//  cursor?.use {
//    if (it.moveToFirst()) {
//      do {
//        set.add(cursor.getLong(COLUMN_WATCH_NEXT_ID_INDEX))
//      } while (it.moveToNext())
//    }
//  }
//  return set
//}
//
