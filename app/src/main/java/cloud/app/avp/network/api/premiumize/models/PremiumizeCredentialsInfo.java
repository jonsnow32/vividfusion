package cloud.app.avp.network.api.premiumize.models;

public class PremiumizeCredentialsInfo {
    private String apikey;
    private long premium_until;

    public long getPremium_until() {
        return premium_until;
    }

    public void setPremium_until(long premium_until) {
        this.premium_until = premium_until;
    }

    public String getAccessToken() {
        return this.apikey;
    }

    public void setAccessToken(String str) {
        this.apikey = str;
    }


    public boolean isValid() {
        if (this.apikey == null || this.apikey.isEmpty()) {
            return false;
        }
        return true;
    }

}
