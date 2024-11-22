package cloud.app.vvf.network.api.premiumize.models;

public class PremiumizeItemDetails {
    /**
     * id : yMrMBrAhaWXBIhI0bXB1Kw
     * name : C.M.2019.Digital.Extras.720p.AMZN.WEB-DL.DDP5.1.H.264-NTG.mkv
     * size : 2146541182
     * created_at : 1560088648
     * transcode_status : finished
     * folder_id : TB5N_KQzrqZ2LAL7LcaTAQ
     * ip : 54.38.46.238
     * acodec : eac3
     * vcodec : h264
     * mime_type : video/x-matroska
     * opensubtitles_hash : ac7e934b3097b39d
     * type : file
     * link : https://julianna.rapidcdn.vip/dl/Un184I3o4fOmMEZBSagamg/1560693861/735120337/5cefa972c32605.07750557/C.M.2019.Digital.Extras.720p.AMZN.WEB-DL.DDP5.1.H.264-NTG.mkv
     * stream_link : https://julianna.rapidcdn.vip/dl/KD2hQGzJ2nfWEodQpklhmQ/1560693861/735120337/5cefa972c32605.07750557_default.mp4/C.M.2019.Digital.Extras.720p.AMZN.WEB-DL.DDP5.1.H.264-NTG.mkv.mp4
     */

    private String id;
    private String name;
    private int size;
    private int created_at;
    private String transcode_status;
    private String folder_id;
    private String ip;
    private String acodec;
    private String vcodec;
    private String mime_type;
    private String opensubtitles_hash;
    private String type;
    private String link;
    private String stream_link;

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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCreated_at() {
        return created_at;
    }

    public void setCreated_at(int created_at) {
        this.created_at = created_at;
    }

    public String getTranscode_status() {
        return transcode_status;
    }

    public void setTranscode_status(String transcode_status) {
        this.transcode_status = transcode_status;
    }

    public String getFolder_id() {
        return folder_id;
    }

    public void setFolder_id(String folder_id) {
        this.folder_id = folder_id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAcodec() {
        return acodec;
    }

    public void setAcodec(String acodec) {
        this.acodec = acodec;
    }

    public String getVcodec() {
        return vcodec;
    }

    public void setVcodec(String vcodec) {
        this.vcodec = vcodec;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public String getOpensubtitles_hash() {
        return opensubtitles_hash;
    }

    public void setOpensubtitles_hash(String opensubtitles_hash) {
        this.opensubtitles_hash = opensubtitles_hash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getStream_link() {
        return stream_link;
    }

    public void setStream_link(String stream_link) {
        this.stream_link = stream_link;
    }
}
