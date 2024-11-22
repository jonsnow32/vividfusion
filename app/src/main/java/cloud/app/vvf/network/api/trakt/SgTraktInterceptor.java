package cloud.app.vvf.network.api.trakt;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.TraktV2Interceptor;

import java.io.IOException;

import javax.inject.Inject;

import dagger.Lazy;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link TraktV2Interceptor} which does not require a {@link
 * TraktV2} instance until intercepting.
 */
public class SgTraktInterceptor implements Interceptor {

    private final Lazy<TraktV2> trakt;

    @Inject
    public SgTraktInterceptor(Lazy<TraktV2> trakt) {
        this.trakt = trakt;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return TraktV2Interceptor.handleIntercept(chain, trakt.get().apiKey(),
                trakt.get().accessToken());
    }
}
