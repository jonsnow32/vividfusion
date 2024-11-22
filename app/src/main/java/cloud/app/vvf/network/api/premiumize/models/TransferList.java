package cloud.app.vvf.network.api.premiumize.models;


import java.util.List;

public class TransferList {

    /**
     * status : success
     * transfers : [{"id":"4pSTroSvZHjwW479Z9vG3Q","name":"The Walking Dead Season 10 Complete 720p AMZN WEB-DL x264 [i_c]","message":null,"status":"finished","progress":0,"folder_id":"5PV4VlG5WNTLpLT8gj6eag","file_id":null,"src":"magnet:?xt=urn:btih:15d48fc7050f549738532dc649f1ca016c72a235&dn=The+Walking+Dead+Season+10+Complete+720p+AMZN+WEB-DL+X264+%5Bi+C%5D&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp://tracker.internetwarriors.net:1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969"}]
     */

    private String status;
    private List<TransfersBean> transfers;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<TransfersBean> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<TransfersBean> transfers) {
        this.transfers = transfers;
    }

    public static class TransfersBean {
        /**
         * id : 4pSTroSvZHjwW479Z9vG3Q
         * name : The Walking Dead Season 10 Complete 720p AMZN WEB-DL x264 [i_c]
         * message : null
         * status : finished
         * progress : 0
         * folder_id : 5PV4VlG5WNTLpLT8gj6eag
         * file_id : null
         * src : magnet:?xt=urn:btih:15d48fc7050f549738532dc649f1ca016c72a235&dn=The+Walking+Dead+Season+10+Complete+720p+AMZN+WEB-DL+X264+%5Bi+C%5D&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp://tracker.internetwarriors.net:1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969
         */

        private String id;
        private String name;
        private String message;
        private String status;
        private double progress;
        private String folder_id;
        private String file_id;
        private String src;

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

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public double getProgress() {
            return progress;
        }

        public void setProgress(double progress) {
            this.progress = progress;
        }

        public String getFolder_id() {
            return folder_id;
        }

        public void setFolder_id(String folder_id) {
            this.folder_id = folder_id;
        }

        public String getFile_id() {
            return file_id;
        }

        public void setFile_id(String file_id) {
            this.file_id = file_id;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

    }
}
