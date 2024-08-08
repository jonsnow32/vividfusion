package cloud.app.avp.network.di;

import android.content.Context;
import android.content.SharedPreferences;

import cloud.app.avp.network.api.thetvdb.AppTheTvdb;
import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes;
import com.uwetrottmann.thetvdb.services.TheTvdbSearch;
import com.uwetrottmann.thetvdb.services.TheTvdbSeries;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@InstallIn(SingletonComponent.class)
@Module
public class TvdbModule {
    @Provides
    TheTvdbEpisodes provideEpisodesService(AppTheTvdb theTvdb) {
        return theTvdb.episodes();
    }

    @Provides
    TheTvdbSearch provideSearch(AppTheTvdb theTvdb) {
        return theTvdb.search();
    }

    @Provides
    TheTvdbSeries provideSeriesService(AppTheTvdb theTvdb) {
        return theTvdb.series();
    }

    @Provides
    AppTheTvdb provideTheTvdb(Context context, OkHttpClient okHttpClient, SharedPreferences sharedPreferences) {
        return new AppTheTvdb(context, okHttpClient, sharedPreferences.getString("pref_tvdb_api_key", "6UMSCJSYNU96S28F"));
    }
}
