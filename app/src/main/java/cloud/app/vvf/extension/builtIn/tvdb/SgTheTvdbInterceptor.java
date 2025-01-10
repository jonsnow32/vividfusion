package cloud.app.vvf.extension.builtIn.tvdb;

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.TheTvdbInterceptor;

import java.io.IOException;

import javax.inject.Inject;

import dagger.Lazy;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link TheTvdbInterceptor} which does not require a {@link
 * TheTvdb} instance until intercepting.
 */
public class SgTheTvdbInterceptor implements Interceptor {

    private final Lazy<TheTvdb> theTvdb;

    @Inject
    public SgTheTvdbInterceptor(Lazy<TheTvdb> theTvdb) {
        this.theTvdb = theTvdb;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return TheTvdbInterceptor.handleIntercept(chain, theTvdb.get().jsonWebToken());
    }
}
