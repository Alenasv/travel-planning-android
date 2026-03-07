import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface Api {

    @GET("data/{filename}")
    fun getJson(
        @retrofit2.http.Path("filename") filename: String
    ): Call<ResponseBody>
}