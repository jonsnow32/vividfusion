package cloud.app.avp.network.api.alldebrid.model;

public class ADPin {

    /**
     * status : success
     * data : {"pin":"8EXC","check":"7297600786a504740ba7d7616936d4bb8181828a","expires_in":600,"user_url":"https://alldebrid.com/pin/?pin=8EXC","base_url":"https://alldebrid.com/pin/","check_url":"https://api.alldebrid.com/v4/pin/check?check=7297600786a504740ba7d7616936d4bb8181828a&pin=8EXC&agent=Cinema-AGENT"}
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
         * pin : 8EXC
         * check : 7297600786a504740ba7d7616936d4bb8181828a
         * expires_in : 600
         * user_url : https://alldebrid.com/pin/?pin=8EXC
         * base_url : https://alldebrid.com/pin/
         * check_url : https://api.alldebrid.com/v4/pin/check?check=7297600786a504740ba7d7616936d4bb8181828a&pin=8EXC&agent=Cinema-AGENT
         */

        private String pin;
        private String check;
        private int expires_in;
        private String user_url;
        private String base_url;
        private String check_url;

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }

        public String getCheck() {
            return check;
        }

        public void setCheck(String check) {
            this.check = check;
        }

        public int getExpires_in() {
            return expires_in;
        }

        public void setExpires_in(int expires_in) {
            this.expires_in = expires_in;
        }

        public String getUser_url() {
            return user_url;
        }

        public void setUser_url(String user_url) {
            this.user_url = user_url;
        }

        public String getBase_url() {
            return base_url;
        }

        public void setBase_url(String base_url) {
            this.base_url = base_url;
        }

        public String getCheck_url() {
            return check_url;
        }

        public void setCheck_url(String check_url) {
            this.check_url = check_url;
        }
    }
}
