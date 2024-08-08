package cloud.app.avp.network.api.premiumize.models;

import java.util.List;

public class ItemDetails {

    /**
     * id : sLaXcUQGv5tE8kaQ3tg53w
     * name : The Walking Dead S10e14 Look At The Flowers.mkv
     * size : 839397958
     * created_at : 1603423972
     * transcode_status : finished
     * folder_id : 5PV4VlG5WNTLpLT8gj6eag
     * acodec : aac
     * vcodec : h264
     * mime_type : video/x-matroska
     * opensubtitles_hash : b356a51c5607d269
     * resx : 720
     * resy : 1280
     * duration : 2715.050000
     * virus_scan : ok
     * audio_track_names : ["eng"]
     * type : file
     * link : https://siena.alphatrid.com/dl/Rn5muuZ4ay55QrgmIrp9UQ/1604039950/735120337/5eba8832a137d1.37721490/The%20Walking%20Dead%20S10e14%20Look%20At%20The%20Flowers.mkv
     * stream_link : https://siena.alphatrid.com/dl/fa7fHEWshkir1hPybnGCsw/1604039950/735120337/5eba8832a137d1.37721490_default.mp4/The%20Walking%20Dead%20S10e14%20Look%20At%20The%20Flowers.mkv.mp4
     * bitrate : 309164.82
     */

    private String id;
    private String name;
    private long size;
    private int created_at;
    private String transcode_status;
    private String folder_id;
    private String acodec;
    private String vcodec;
    private String mime_type;
    private String opensubtitles_hash;
    private String resx;
    private String resy;
    private String duration;
    private String virus_scan;
    private String type;
    private String link;
    private String stream_link;
    private double bitrate;
    private List<String> audio_track_names;

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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
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

    public String getResx() {
        return resx;
    }

    public void setResx(String resx) {
        this.resx = resx;
    }

    public String getResy() {
        return resy;
    }

    public void setResy(String resy) {
        this.resy = resy;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getVirus_scan() {
        return virus_scan;
    }

    public void setVirus_scan(String virus_scan) {
        this.virus_scan = virus_scan;
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

    public double getBitrate() {
        return bitrate;
    }

    public void setBitrate(double bitrate) {
        this.bitrate = bitrate;
    }

    public List<String> getAudio_track_names() {
        return audio_track_names;
    }

    public void setAudio_track_names(List<String> audio_track_names) {
        this.audio_track_names = audio_track_names;
    }
}
