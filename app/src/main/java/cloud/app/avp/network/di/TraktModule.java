package cloud.app.avp.network.di;

import android.content.Context;
import android.content.SharedPreferences;

import cloud.app.avp.network.api.trakt.AppTrakt;
import com.uwetrottmann.trakt5.services.Episodes;
import com.uwetrottmann.trakt5.services.Movies;
import com.uwetrottmann.trakt5.services.Search;
import com.uwetrottmann.trakt5.services.Shows;
import com.uwetrottmann.trakt5.services.Sync;
import com.uwetrottmann.trakt5.services.Users;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@InstallIn({SingletonComponent.class})
@Module
public class TraktModule {
    @Provides
    Episodes provideEpisodes(AppTrakt trakt) {
        return trakt.episodes();
    }

    @Provides
    Movies provideMovies(AppTrakt trakt) {
        return trakt.movies();
    }

    @Provides
    Shows provideShows(AppTrakt trakt) {
        return trakt.shows();
    }

    @Provides
    Search provideSearch(AppTrakt trakt) {
        return trakt.search();
    }

    @Provides
    Sync provideSync(AppTrakt trakt) {
        return trakt.sync();
    }

    @Provides
    Users provideUsers(AppTrakt trakt) {
        return trakt.users();
    }

    @Provides
    AppTrakt provideTrakt(Context context, OkHttpClient okHttpClient, SharedPreferences preferences) {
        return new AppTrakt(context, okHttpClient, "f6828254ed50cb58c2c9ee321f8ed7fe88512a2b52a6831feb7ea509fad13ec2", "4a00040c96b943f5d7203916c3f2499d4583cea0eb74016800c792b1152d67c1", preferences);
    }
}
