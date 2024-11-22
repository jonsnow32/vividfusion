package cloud.app.vvf.network.api.alldebrid.model.Torrent;

import java.util.List;

public class ADTorrentUpload {

    /**
     * status : success
     * data : {"magnets":[{"magnet":"magnet:?xt=urn:btih:3A842783e3005495d5d1637f5364b59343c7844707&dn=ubuntu-18.04.2-live-server-amd64.iso","hash":"3A842783e3005495d5d1637f5364b59343c7844707","name":"ubuntu-18.04.2-live-server-amd64.iso","size":48216647,"id":123456,"ready":false}]}
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
        private List<MagnetsBean> magnets;

        public List<MagnetsBean> getMagnets() {
            return magnets;
        }

        public void setMagnets(List<MagnetsBean> magnets) {
            this.magnets = magnets;
        }

        public static class MagnetsBean {
            /**
             * magnet : magnet:?xt=urn:btih:3A842783e3005495d5d1637f5364b59343c7844707&dn=ubuntu-18.04.2-live-server-amd64.iso
             * hash : 3A842783e3005495d5d1637f5364b59343c7844707
             * name : ubuntu-18.04.2-live-server-amd64.iso
             * size : 48216647
             * id : 123456
             * ready : false
             */

            private String magnet;
            private String hash;
            private String name;
            private long size;
            private int id;
            private boolean ready;

            public String getMagnet() {
                return magnet;
            }

            public void setMagnet(String magnet) {
                this.magnet = magnet;
            }

            public String getHash() {
                return hash;
            }

            public void setHash(String hash) {
                this.hash = hash;
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

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public boolean isReady() {
                return ready;
            }

            public void setReady(boolean ready) {
                this.ready = ready;
            }

        }
    }
}
