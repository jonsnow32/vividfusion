package cloud.app.vvf.network.api.alldebrid.model;

import java.util.List;

public class ADResponceLink {

    /**
     * status : success
     * data : {"link":"https://8i7jx5.alldebridro.ovh/dl/1siztln4928/Ad.Astra.2019.HDRip.AC3.x264-CMRG.mkv","host":"generic","filename":"Ad.Astra.2019.HDRip.AC3.x264-CMRG.mkv","streaming":[],"paws":true,"filesize":1452021255,"streams":[{"quality":360,"ext":"mp4","filesize":584536064,"name":"eng","link":"https://www69.uptostream.com/1zuyo30e63f/360/0/video.mp4","id":"360-eng"}],"id":"1siztln4928"}
     */

    private String status;
    private Error error;
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

    public Error getError() {
        return error;
    }

    public static class DataBean {
        /**
         * link : https://8i7jx5.alldebridro.ovh/dl/1siztln4928/Ad.Astra.2019.HDRip.AC3.x264-CMRG.mkv
         * host : generic
         * filename : Ad.Astra.2019.HDRip.AC3.x264-CMRG.mkv
         * streaming : []
         * paws : true
         * filesize : 1452021255
         * streams : [{"quality":360,"ext":"mp4","filesize":584536064,"name":"eng","link":"https://www69.uptostream.com/1zuyo30e63f/360/0/video.mp4","id":"360-eng"}]
         * id : 1siztln4928
         */

        private String link;
        private String host;
        private String filename;
        private boolean paws;
        private long filesize;
        private String id;
        private List<?> streaming;
        private List<StreamsBean> streams;

        public StreamsBean getMaxQuality() {
            StreamsBean streamsBean = streams.get(0);
            for (StreamsBean bean : streams)
                if (bean.getQuality() > streamsBean.getQuality())
                    streamsBean = bean;


            return streamsBean;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public boolean isPaws() {
            return paws;
        }

        public void setPaws(boolean paws) {
            this.paws = paws;
        }

        public long getFilesize() {
            return filesize;
        }

        public void setFilesize(long filesize) {
            this.filesize = filesize;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<?> getStreaming() {
            return streaming;
        }

        public void setStreaming(List<?> streaming) {
            this.streaming = streaming;
        }

        public List<StreamsBean> getStreams() {
            return streams;
        }

        public void setStreams(List<StreamsBean> streams) {
            this.streams = streams;
        }

        public static class StreamsBean {
            /**
             * quality : 360
             * ext : mp4
             * filesize : 584536064
             * name : eng
             * link : https://www69.uptostream.com/1zuyo30e63f/360/0/video.mp4
             * id : 360-eng
             */

            private int quality;
            private String ext;
            private long filesize;
            private String name;
            private String link;
            private String id;

            public int getQuality() {
                return quality;
            }

            public void setQuality(int quality) {
                this.quality = quality;
            }

            public String getExt() {
                return ext;
            }

            public void setExt(String ext) {
                this.ext = ext;
            }

            public long getFilesize() {
                return filesize;
            }

            public void setFilesize(long filesize) {
                this.filesize = filesize;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }
    }
}
