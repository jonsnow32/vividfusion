package cloud.app.vvf.network.api.alldebrid.model;

import java.util.List;

public class ADstatusSingle {

    /**
     * status : success
     * data : {"magnets":[{"id":92308829,"filename":"Ghost Whisperer All Seasons  1 - 5 _ Pillz","size":39246855527,"hash":"b411721b31e435454bfcbd94065705cd6c4f660d","status":"Ready","statusCode":4,"downloaded":39246855527,"uploaded":39246855527,"seeders":0,"downloadSpeed":0,"uploadSpeed":0,"uploadDate":1603272075,"completionDate":1603272075,"links":[{"link":"https://uptobox.com/y3hvgp92ow1t","filename":"Episode 01 - Pilot.avi","size":365644398,"files":["Ghost Whisperer - Season 1/Episode 01 - Pilot.avi"]}]}]}
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
        private MagnetsBean magnets;

        public MagnetsBean getMagnets() {
            return magnets;
        }

        public void setMagnets(MagnetsBean magnets) {
            this.magnets = magnets;
        }

        public static class MagnetsBean {
            /**
             * id : 92308829
             * filename : Ghost Whisperer All Seasons  1 - 5 _ Pillz
             * size : 39246855527
             * hash : b411721b31e435454bfcbd94065705cd6c4f660d
             * status : Ready
             * statusCode : 4
             * downloaded : 39246855527
             * uploaded : 39246855527
             * seeders : 0
             * downloadSpeed : 0
             * uploadSpeed : 0
             * uploadDate : 1603272075
             * completionDate : 1603272075
             * links : [{"link":"https://uptobox.com/y3hvgp92ow1t","filename":"Episode 01 - Pilot.avi","size":365644398,"files":["Ghost Whisperer - Season 1/Episode 01 - Pilot.avi"]}]
             */

            private int id;
            private String filename;
            private long size;
            private String hash;
            private String status;
            private int statusCode;
            private long downloaded;
            private long uploaded;
            private int seeders;
            private int downloadSpeed;
            private int uploadSpeed;
            private int uploadDate;
            private int completionDate;
            private List<LinksBean> links;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getFilename() {
                return filename;
            }

            public void setFilename(String filename) {
                this.filename = filename;
            }

            public long getSize() {
                return size;
            }

            public void setSize(long size) {
                this.size = size;
            }

            public String getHash() {
                return hash;
            }

            public void setHash(String hash) {
                this.hash = hash;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public int getStatusCode() {
                return statusCode;
            }

            public void setStatusCode(int statusCode) {
                this.statusCode = statusCode;
            }

            public long getDownloaded() {
                return downloaded;
            }

            public void setDownloaded(long downloaded) {
                this.downloaded = downloaded;
            }

            public long getUploaded() {
                return uploaded;
            }

            public void setUploaded(long uploaded) {
                this.uploaded = uploaded;
            }

            public int getSeeders() {
                return seeders;
            }

            public void setSeeders(int seeders) {
                this.seeders = seeders;
            }

            public int getDownloadSpeed() {
                return downloadSpeed;
            }

            public void setDownloadSpeed(int downloadSpeed) {
                this.downloadSpeed = downloadSpeed;
            }

            public int getUploadSpeed() {
                return uploadSpeed;
            }

            public void setUploadSpeed(int uploadSpeed) {
                this.uploadSpeed = uploadSpeed;
            }

            public int getUploadDate() {
                return uploadDate;
            }

            public void setUploadDate(int uploadDate) {
                this.uploadDate = uploadDate;
            }

            public int getCompletionDate() {
                return completionDate;
            }

            public void setCompletionDate(int completionDate) {
                this.completionDate = completionDate;
            }

            public List<LinksBean> getLinks() {
                return links;
            }

            public void setLinks(List<LinksBean> links) {
                this.links = links;
            }


            private int getprogress() {
                if (size <= 0)
                    return 0;

                if (status.contains("Uploading")) {
                    return (int) (uploaded * 100 / size);
                }
                return (int) (downloaded * 100 / size);
            }

            public static class LinksBean {
                /**
                 * link : https://uptobox.com/y3hvgp92ow1t
                 * filename : Episode 01 - Pilot.avi
                 * size : 365644398
                 * files : ["Ghost Whisperer - Season 1/Episode 01 - Pilot.avi"]
                 */

                private String link;
                private String filename;
                private long size;
                private List<FilesLinkBean> files;

                public String getLink() {
                    return link;
                }

                public void setLink(String link) {
                    this.link = link;
                }

                public String getFilename() {
                    return filename;
                }

                public void setFilename(String filename) {
                    this.filename = filename;
                }

                public long getSize() {
                    return size;
                }

                public void setSize(long size) {
                    this.size = size;
                }

                public List<FilesLinkBean> getFiles() {
                    return files;
                }

                public void setFiles(List<FilesLinkBean> files) {
                    this.files = files;
                }
            }

            public static class FilesLinkBean {
                private String n;

                public String getN() {
                    return n;
                }

                public void setN(String n) {
                    this.n = n;
                }
            }
        }
    }
}
