package cloud.app.vvf.plugin.tvdb;

/**
 * Thrown when a {@link } trakt operation fails.
 */
public class TvdbTraktException extends TvdbException {

    public TvdbTraktException(String message) {
        super(message, null, Service.TRAKT, false);
    }
}
