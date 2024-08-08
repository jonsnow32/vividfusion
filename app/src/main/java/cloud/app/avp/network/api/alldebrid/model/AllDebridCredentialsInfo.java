package cloud.app.avp.network.api.alldebrid.model;

public class AllDebridCredentialsInfo {
    private String pin;
    private long expires_in;
    private boolean activated;
    private String apikey;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    public String getApikey() {
        return this.apikey;
    }

    public void setApiKey(String str, long expired_in) {
        this.apikey = str;
        this.expires_in = expired_in;
    }


    public boolean isValid() {
        if (this.apikey == null || this.apikey.isEmpty()) {
            return false;
        }
        return true;
    }

}
