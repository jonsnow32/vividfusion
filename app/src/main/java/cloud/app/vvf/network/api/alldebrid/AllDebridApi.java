package cloud.app.vvf.network.api.alldebrid;

import cloud.app.vvf.network.api.alldebrid.model.ADGetTokenResult;
import cloud.app.vvf.network.api.alldebrid.model.ADInstanceTorrent;
import cloud.app.vvf.network.api.alldebrid.model.ADPin;
import cloud.app.vvf.network.api.alldebrid.model.ADResponceLink;
import cloud.app.vvf.network.api.alldebrid.model.ADUserInfo;
import cloud.app.vvf.network.api.alldebrid.model.ADstatusSingle;
import cloud.app.vvf.network.api.alldebrid.model.Torrent.ADTorrentUpload;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AllDebridApi {

    @GET("/v4/pin/get")
    Call<ADPin> getPin();

    @GET("/v4/pin/check")
    Call<ADGetTokenResult> check(@Query("check") String checkCode, @Query("pin") String pin);


    @GET("/v4/user")
    Call<ADUserInfo> userInfo();

    @GET("/v4/link/unlock")
    Call<ADResponceLink> getdownloadlink(@Query("link") String link);

    @GET("/v4/magnet/instant")
    Call<ADInstanceTorrent> getAllDebridInstance(@Query("magnets[]=") List<String> link);

    @GET("/v4/magnet/status")
    Call<ADstatusSingle> status(@Query("id") String magnetID);


    @GET("/v4/magnet/delete")
    Call<ResponseBody> delete(@Query("id") String magnetID);

    @FormUrlEncoded
    @POST("/v4/magnet/upload")
    Call<ADTorrentUpload> uploadMagnet(@Field("magnets[]=") List<String> link);
}
