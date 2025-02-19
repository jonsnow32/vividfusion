package cloud.app.vvf.network.api.voting

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VotingService {
  @GET("vvf-{type}/vote/{className}")
  fun getVotes(
    @Path("type") extensionType: String,
    @Path("className") extensionClassName: String,
    @Query("readOnly") readOnly: Boolean = true
  ):  Call<VotingResult>

  @GET("vvf-{type}/vote/{className}")
  fun vote(
    @Path("type") extensionType: String,
    @Path("className") extensionClassName: String
  ): Call<VotingResult>
}
