package cloud.app.avp.network.api.premiumize.models;

import java.util.List;

public class PremiumizeDirectDL {

    /**
     * status : success
     * location : https://server.com/path/file.ext
     * filename : file.ext
     * filesize : 123123123
     * content : [{"path":"folder/file1.jpg","size":123123123,"link":"https://server.com/path/file.ext","stream_link":"https://server.com/path/file.ext","transcode_status":"finished"}]
     */

    private String status;
    private List<ContentBean> content;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ContentBean> getContent() {
        return content;
    }

    public void setContent(List<ContentBean> content) {
        this.content = content;
    }

    public static class ContentBean {
        /**
         * path : folder/file1.jpg
         * size : 123123123
         * link : https://server.com/path/file.ext
         * stream_link : https://server.com/path/file.ext
         * transcode_status : finished
         */

        private String path;
        private long size;
        private String link;
        private String stream_link;
        private String transcode_status;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getStream_link() {
            if (stream_link == null || stream_link.isEmpty())
                return link;
            return stream_link;
        }

        public void setStream_link(String stream_link) {
            this.stream_link = stream_link;
        }

        public String getTranscode_status() {
            return transcode_status;
        }

        public void setTranscode_status(String transcode_status) {
            this.transcode_status = transcode_status;
        }
    }
}

