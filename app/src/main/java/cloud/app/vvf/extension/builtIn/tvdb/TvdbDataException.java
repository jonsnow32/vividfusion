package cloud.app.vvf.extension.builtIn.tvdb;

public class TvdbDataException extends TvdbException {

    public TvdbDataException(String message) {
        this(message, null);
    }

    public TvdbDataException(String message, Throwable throwable) {
        super(message, throwable, Service.DATA, false);
    }
}
