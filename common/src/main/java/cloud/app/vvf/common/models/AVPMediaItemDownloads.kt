package cloud.app.vvf.common.models

/**
 * Extension functions to add download capabilities to AVPMediaItem
 * Note: These functions will work with DownloadManager when it's injected in the app module
 */

/**
 * Check if this media item can be downloaded
 */
fun AVPMediaItem.isDownloadable(): Boolean {
    return when (this) {
        is AVPMediaItem.MovieItem -> true
        is AVPMediaItem.EpisodeItem -> true
        is AVPMediaItem.VideoItem -> true
        is AVPMediaItem.TrackItem -> true
        else -> false
    }
}

/**
 * Get the download URL for this media item
 * Note: This would need to be implemented based on your media source providers
 */
fun AVPMediaItem.getDownloadUrl(quality: String = "default"): String? {
    return when (this) {
        is AVPMediaItem.MovieItem -> {
            // Extract download URL from movie sources
            // This would integrate with your existing video source extractors
            // For now, using a placeholder - you'd need to integrate with your video sources
            null // movie.sources?.firstOrNull()?.url
        }
        is AVPMediaItem.EpisodeItem -> {
            // Extract download URL from episode sources
            // This would integrate with your existing video source extractors
            null // episode.sources?.firstOrNull()?.url
        }
        is AVPMediaItem.VideoItem -> {
            // Direct video URL
            video.uri
        }
        is AVPMediaItem.TrackItem -> {
            // Direct audio URL
            track.uri
        }
        else -> null
    }
}

/**
 * Get the media type for download categorization
 */
fun AVPMediaItem.getMediaType(): String {
    return when (this) {
        is AVPMediaItem.MovieItem -> "movie"
        is AVPMediaItem.EpisodeItem -> "episode"
        is AVPMediaItem.VideoItem -> "video"
        is AVPMediaItem.TrackItem -> "audio"
        else -> "unknown"
    }
}

/**
 * Get display name for downloads
 */
fun AVPMediaItem.getDownloadDisplayName(): String {
    return when (this) {
        is AVPMediaItem.MovieItem -> "${movie.generalInfo.title} (${releaseYear?.toString() ?: "Unknown"})"
        is AVPMediaItem.EpisodeItem -> {
            val show = seasonItem.showItem.show.generalInfo.title
            val season = seasonItem.season.number
            val episode = episode.episodeNumber
            "$show - S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
        }
        is AVPMediaItem.VideoItem -> video.title ?: "Unknown Title"
        is AVPMediaItem.TrackItem -> track.title
        else -> title ?: "Unknown Title"
    }
}

/**
 * Get poster/thumbnail for downloads
 */
fun AVPMediaItem.getDownloadThumbnail(): String? {
    return when (this) {
        is AVPMediaItem.MovieItem -> movie.generalInfo.poster
        is AVPMediaItem.EpisodeItem -> seasonItem.showItem.show.generalInfo.poster
        is AVPMediaItem.VideoItem -> video.thumbnailUri
        is AVPMediaItem.TrackItem -> track.cover
        else -> null
    }
}
