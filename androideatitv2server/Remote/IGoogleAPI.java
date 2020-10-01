package com.ds.androideatitv2server.Remote;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IGoogleAPI {
    @GET("maps/api/directions/json")
    Observable<String> getDirections(
            @Query("mode") String mode,
            @Query("transit_routing_preference") String transit_routing,
            @Query("origin") String from,
            @Query("destination") String to,
            @Query("key") String key);
    }

