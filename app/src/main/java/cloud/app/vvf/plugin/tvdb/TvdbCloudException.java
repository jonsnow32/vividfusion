package cloud.app.vvf.plugin.tvdb;


public class TvdbCloudException extends TvdbException {

    public TvdbCloudException(String message, Throwable throwable) {
        super(message, throwable, Service.HEXAGON, false);
    }
}
