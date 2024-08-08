package cloud.app.avp.network.api.premiumize.models;

public class TransferCreate {

    /**
     * status : success
     * id : string
     * name : example.jpg
     * type : string
     */

    private String status;
    private String id;
    private String name;
    private String type;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

}
