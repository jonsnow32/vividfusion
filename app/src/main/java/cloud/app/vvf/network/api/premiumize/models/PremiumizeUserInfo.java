package cloud.app.vvf.network.api.premiumize.models;

import com.google.gson.annotations.SerializedName;

import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;

public class PremiumizeUserInfo {
    /**
     * status : success
     * customer_id : 735120337
     * premium_until : 1562501687
     * limit_used : 0
     * space_used : 0
     */

    private String status;
    private String customer_id;
    @SerializedName("premium_until")
    private Object premium_until;
    private float limit_used;
    private float space_used;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public Object getPremium_until() {
        return premium_until;
    }

    public void setPremium_until(Object premium_until) {
        this.premium_until = premium_until;
    }

    public long getLongPremium_until() {
        try {
            return (long) Double.parseDouble(premium_until.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public float getLimit_used() {
        return limit_used;
    }

    public void setLimit_used(float limit_used) {
        this.limit_used = limit_used;
    }

    public float getSpace_used() {
        return space_used;
    }

    public void setSpace_used(float space_used) {
        this.space_used = space_used;
    }

    @Override
    public String toString() {
        OffsetDateTime offsetDateTime = null;
        try {
            offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond((long) Double.parseDouble(premium_until.toString()), 0), ZoneOffset.systemDefault());
        } catch (Exception e) {

        }
        return "Premiumize authorized " +
                "\nCustomer id: " + customer_id +
                "\nPremium until: " + (offsetDateTime == null ? premium_until.toString() : offsetDateTime.toLocalDateTime().toString()) +
                "\nLimit used: " + limit_used +
                "\nSpace used: " + space_used
                ;
    }
}
