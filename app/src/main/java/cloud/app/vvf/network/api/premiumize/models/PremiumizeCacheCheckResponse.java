package cloud.app.vvf.network.api.premiumize.models;

import java.util.ArrayList;
import java.util.List;

public class PremiumizeCacheCheckResponse {
    public String[] filename;
    public String[] filesize;
    public String[] response;
    public String status;
    public String[] transcoded;
    private List<CacheCheckResponseSingle> consolidatedResponse;

    public CacheCheckResponseSingle getFirst() {
        return (consolidate() == null || consolidate().size() <= 0) ? null : (CacheCheckResponseSingle) consolidate().get(0);
    }

    public List<CacheCheckResponseSingle> consolidate() {
        if (!"success".equals(this.status) || this.response == null || this.response.length <= 0) {
            return null;
        }
        if (this.consolidatedResponse == null) {
            this.consolidatedResponse = new ArrayList();
            for (int i = 0; i < this.response.length; i++) {
                this.consolidatedResponse.add(new CacheCheckResponseSingle(this, i));
            }
        }
        return this.consolidatedResponse;
    }

    public class CacheCheckResponseSingle {
        public String filename;
        public Long filesize;
        public boolean response;
        public String transcoded;

        public CacheCheckResponseSingle(PremiumizeCacheCheckResponse cacheCheckResponse, int i) {
            boolean z = cacheCheckResponse.response != null && cacheCheckResponse.response.length > i && "true".equals(cacheCheckResponse.response[i]);
            this.response = z;
            String str = null;
            String str2 = (cacheCheckResponse.transcoded == null || cacheCheckResponse.transcoded.length <= i) ? null : cacheCheckResponse.transcoded[i];
            this.transcoded = str2;
            if (cacheCheckResponse.filename != null && cacheCheckResponse.filename.length > i) {
                str = cacheCheckResponse.filename[i];
            }
            this.filename = str;
            if (cacheCheckResponse.filesize != null && cacheCheckResponse.filesize.length > i) {
                try {
                    this.filesize = Long.valueOf(Long.parseLong(cacheCheckResponse.filesize[i]));
                } catch (Exception unused) {
                }
            }
        }
    }
}
