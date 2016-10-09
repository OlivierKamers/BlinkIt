package com.choosemuse.example.libmuse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

interface MessageSend {
    @GET("/message/{id_from}/{id_to}/{message}")
    Call<Object> sendMessage(
            @Path("id_from") int id_from,
            @Path("id_to") int id_to,
            @Path("message") String message);
}
