package org.tensorflow.lite.examples.detection.server_communication;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitService {

//        String URL = "http://jsonplaceholder.typicode.com";
//     String URL = "http://webhook.site";
    String URL = "http://101.101.216.124";


    @GET("/api/v1/crack/{data}")
    Call<SendObject> getData(@Path("data") String param);

    @GET("/api/v1/structure/{data}")
    Call<List<StructureObject>> getStructureData(@Path("data") String param);


    @POST("/api/v1/crack")
    Call<Integer> postData(
            @Body SendObject param
    );
}
