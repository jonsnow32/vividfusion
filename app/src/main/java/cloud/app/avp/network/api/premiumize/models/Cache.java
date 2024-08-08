package cloud.app.avp.network.api.premiumize.models;

import java.util.List;

public class Cache {

    /**
     * status : success
     * response : [true]
     * transcoded : [true]
     * filename : ["string"]
     * filesize : ["string"]
     */

    private String status;
    private List<Boolean> response;
    private List<Boolean> transcoded;
    private List<String> filename;
    private List<String> filesize;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Boolean> getResponse() {
        return response;
    }

    public void setResponse(List<Boolean> response) {
        this.response = response;
    }

    public List<Boolean> getTranscoded() {
        return transcoded;
    }

    public void setTranscoded(List<Boolean> transcoded) {
        this.transcoded = transcoded;
    }

    public List<String> getFilename() {
        return filename;
    }

    public void setFilename(List<String> filename) {
        this.filename = filename;
    }

    public List<String> getFilesize() {
        return filesize;
    }

    public void setFilesize(List<String> filesize) {
        this.filesize = filesize;
    }
}
