package cloud.app.vvf.network.api.premiumize.models;

public class PremiumizeTransferPesponse {
    /**
     * status : error
     * error : duplicate
     * id : PHgF4hq1UoM2QIfr30LjRw
     * message : You already have this job added.
     */
    /**
     * "status": "success",
     * "type": "savetocloud",
     * "id": "GPSpr3Fxe5RbVr0_0mKcaQ",
     * "name": "C.M.2019.Digital.Extras.720p.AMZN.WEB-DL.DDP5.1.H.264-NTG.mkv"
     */

    private String status;
    private String error;
    private String name;
    private String type;
    private String id;
    private String message;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
