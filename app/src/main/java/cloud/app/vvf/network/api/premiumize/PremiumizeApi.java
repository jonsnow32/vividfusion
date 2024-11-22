package cloud.app.vvf.network.api.premiumize;


import cloud.app.vvf.network.api.premiumize.models.FolderList;
import cloud.app.vvf.network.api.premiumize.models.ItemDetails;
import cloud.app.vvf.network.api.premiumize.models.PremiumizeCacheCheckResponse;
import cloud.app.vvf.network.api.premiumize.models.PremiumizeCreateFolderPesponse;
import cloud.app.vvf.network.api.premiumize.models.PremiumizeDirectDL;
import cloud.app.vvf.network.api.premiumize.models.PremiumizeTorrentDirectDL;
import cloud.app.vvf.network.api.premiumize.models.PremiumizeUserInfo;
import cloud.app.vvf.network.api.premiumize.models.TransferCreate;
import cloud.app.vvf.network.api.premiumize.models.TransferList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PremiumizeApi {
    @FormUrlEncoded
    @POST("/api/account/info")
    Call<PremiumizeUserInfo> getPremiumizeUserInfo(@Field("apikey") String apikey);

    @GET("/api/transfer/list")
    Call<TransferList> transferlist(@Query("apikey") String apikey);

    @FormUrlEncoded
    @POST("/api/transfer/directdl")
    Call<PremiumizeDirectDL> getPremiumizeDirectDL(@Field("apikey") String apikey, @Field("src") String src);

    @FormUrlEncoded
    @POST("/api/transfer/delete")
    Call<ResponseBody> transferdelete(@Field("apikey") String apikey, @Field("id") String id);

    @FormUrlEncoded
    @POST("/api/transfer/create")
    Call<TransferCreate> transferCreate(@Field("apikey") String apikey, @Field("src") String src);

    @FormUrlEncoded
    @POST("/api/transfer/directdl")
    Call<PremiumizeTorrentDirectDL> getPremiumizeTorrentDirectDL(@Field("apikey") String apikey, @Field("src") String src);

    @FormUrlEncoded
    @POST("/api/cache/check")
    Call<PremiumizeCacheCheckResponse> getPremiumizeCacheCheckResponse(@Field("apikey") String apikey, @Query("items[]") String[] items);

    @FormUrlEncoded
    @POST("/api/transfer/create")
    Call<ResponseBody> getPremiumizeTransferPesponse(@Field("apikey") String apikey, @Field("src") String src);

    @FormUrlEncoded
    @POST("/api/folder/create")
    Call<PremiumizeCreateFolderPesponse> getPremiumizeCreateFolderPesponse(@Field("apikey") String apikey, @Field("name") String name);

    @FormUrlEncoded
    @POST("/api/folder/delete")
    Call<ResponseBody> folderDelete(@Field("apikey") String apikey, @Field("id") String id);

    @GET("/api/folder/list")
    Call<FolderList> folderList(@Query("apikey") String apikey, @Query("id") String id, @Query("includebreadcrumbs") Boolean includebreadcrumbs);

    @FormUrlEncoded
    @POST("/api/item/delete")
    Call<ResponseBody> itemDelete(@Field("apikey") String apikey, @Field("id") String id);

    @GET("/api/item/details")
    Call<ItemDetails> itemDetails(@Query("apikey") String apikey, @Query("id") String id);


}
