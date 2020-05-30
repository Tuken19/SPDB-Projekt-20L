package com.example.spdb;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OverpassServiceNew {
    @GET("/api/interpreter")
    Call<OverpassQueryResultNew> interpreter(@Query("data") String data);
}