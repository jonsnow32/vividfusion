package cloud.app.vvf.network.di;

import android.content.SharedPreferences;

import cloud.app.vvf.extension.builtIn.tmdb.AppTmdb;
import cloud.app.vvf.extension.builtIn.tmdb.ExtendService;
import com.uwetrottmann.tmdb2.services.ConfigurationService;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@InstallIn(SingletonComponent.class)
@Module
public class TmdbModule {
    @Provides
    ConfigurationService provideConfigurationService(AppTmdb tmdb) {
        return tmdb.configurationService();
    }
    @Provides
    ExtendService provideImageService(AppTmdb tmdb) {
        return tmdb.extendService();
    }


}
