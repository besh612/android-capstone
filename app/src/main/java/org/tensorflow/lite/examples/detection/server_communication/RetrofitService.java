package org.tensorflow.lite.examples.detection.server_communication;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitService {

    //    String URL = "http://jsonplaceholder.typicode.com";
     String URL = "https://webhook.site";
//    String URL = "http://101.101.216.124";

    // @GET: HTTP method(GET, POST, PUT, DELETE, HEAD)중 작업 선택
    // "posts/": 전체 URI에서 URL을 제외한 End point(URI)
    // {post}: @Path에 대입할 변수 => @Path("post")의 post
    // @Path("post") String post: 매개변수 post가 @Path("post")에 의해 @GET내부 {post}에 대입된다.

    // EX) https://jsonplaceholder.typicode.com/posts/1
    // https://jsonplaceholder.typicode.com/: URL = 프로토콜(http://) + URL => baseURL이 되는 부분
    // posts/1: End point = URI

    // Call<PostResult>: 반환타입 => Call은 응답이 오면 Callback으로 불려질 타입을 의미
    // PostResult: 요청 GET에 대한 응답데이터를 받아서 객체화할 DTO클래스

    @GET("posts/{data}")
    Call<SendObject> getData(@Path("data") String param);


    // POST방식
    // @FieldMap HashMap<String, Object> param
    //Field형식을 통해 넘기는 값이 여러개일때 FieldMap사용
    //@FormUrlEncoded: Field형식 사용시 Form이 encoding되어야하므로 작성
    @FormUrlEncoded
//    @POST("/posts")
//    @POST("/api/v1/crack/{i}")
    @POST("/fb06c77e-afe3-45e1-872b-a327ac5ec9f6")
//    Call<Data> postData(@FieldMap HashMap<String, Object> param);
        Call<SendObject> postData(@FieldMap HashMap<String, Object> param);
}
