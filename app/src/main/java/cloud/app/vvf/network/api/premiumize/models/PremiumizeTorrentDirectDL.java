package cloud.app.vvf.network.api.premiumize.models;

import java.util.List;

public class PremiumizeTorrentDirectDL {
    /**
     * status : success
     * content : [{"path":"Captain.Marvel.2019.4K.HDR.2160p.mkv","size":"13106015797","link":"https://zara.bufferless.download/dl/o7uCtj7D3hgZ9ReM4Vx7ew/1560702491/735120337/5cecdc9f952db3.75769784/Captain.Marvel.2019.4K.HDR.2160p.mkv","stream_link":"https://zara.bufferless.download/dl/bABovt17n7BR5alNp-n1ZA/1560702491/735120337/5cecdc9f952db3.75769784_default.mp4/Captain.Marvel.2019.4K.HDR.2160p.mkv.mp4","transcode_status":"finished"}]
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
         * path : Captain.Marvel.2019.4K.HDR.2160p.mkv
         * size : 13106015797
         * link : https://zara.bufferless.download/dl/o7uCtj7D3hgZ9ReM4Vx7ew/1560702491/735120337/5cecdc9f952db3.75769784/Captain.Marvel.2019.4K.HDR.2160p.mkv
         * stream_link : https://zara.bufferless.download/dl/bABovt17n7BR5alNp-n1ZA/1560702491/735120337/5cecdc9f952db3.75769784_default.mp4/Captain.Marvel.2019.4K.HDR.2160p.mkv.mp4
         * transcode_status : finished
         */

        private String path;
        private String size;
        private String link;
        private String stream_link;
        private String transcode_status;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
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

        public String getTranscode_status() {
            return transcode_status;
        }

        public void setTranscode_status(String transcode_status) {
            this.transcode_status = transcode_status;
        }
    }


}
