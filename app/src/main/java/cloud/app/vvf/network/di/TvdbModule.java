package cloud.app.vvf.network.di;

import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes;
import com.uwetrottmann.thetvdb.services.TheTvdbSearch;
import com.uwetrottmann.thetvdb.services.TheTvdbSeries;

import cloud.app.vvf.plugin.tvdb.AppTheTvdb;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

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

//    @Provides
//    AppTheTvdb provideTheTvdb(Context context, OkHttpClient okHttpClient, SharedPreferences sharedPreferences) {
//        return new AppTheTvdb(context, okHttpClient, sharedPreferences.getString("pref_tvdb_api_key", "6UMSCJSYNU96S28F"));
//    }
}
