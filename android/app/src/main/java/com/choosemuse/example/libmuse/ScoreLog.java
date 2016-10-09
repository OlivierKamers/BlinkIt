package com.choosemuse.example.libmuse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

interface ScoreLog {
    @GET("/score/{score}/{phone}/{name}")
    Call<ScoreResponse> scoreResponse(
            @Path("score") int score,
            @Path("phone") String phone,
            @Path("name") String name);
}