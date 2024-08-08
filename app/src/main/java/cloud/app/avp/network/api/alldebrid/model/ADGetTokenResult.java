package cloud.app.avp.network.api.alldebrid.model;

public class ADGetTokenResult {


    /**
     * status : success
     * data : {"apikey":"t07xybrl6I1JUvNke8wO","activated":true,"expires_in":488}
     */

    private String status;
    private DataBean data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * apikey : t07xybrl6I1JUvNke8wO
         * activated : true
         * expires_in : 488
         */

        private String apikey;
        private boolean activated;
        private int expires_in;

        public String getApikey() {
            return apikey;
        }

        public void setApikey(String apikey) {
            this.apikey = apikey;
        }

        public boolean isActivated() {
            return activated;
        }

        public void setActivated(boolean activated) {
            this.activated = activated;
        }

        public int getExpires_in() {
            return expires_in;
        }

        public void setExpires_in(int expires_in) {
            this.expires_in = expires_in;
        }
    }
}
