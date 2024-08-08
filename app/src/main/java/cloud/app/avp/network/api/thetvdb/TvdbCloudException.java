package cloud.app.avp.network.api.thetvdb;


public class TvdbCloudException extends TvdbException {

    public TvdbCloudException(String message, Throwable throwable) {
        super(message, throwable, Service.HEXAGON, false);
    }
}
