package com.example.spdb;

import hu.supercluster.overpasser.adapter.OverpassService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OverpassServiceProviderNew {
    private static OverpassServiceNew service;

    public static OverpassServiceNew get() {
        if (service == null) {
            service = createService();
        }

        return service;
    }

    private static OverpassServiceNew createService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://overpass-api.de")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(OverpassServiceNew.class);
    }
}
