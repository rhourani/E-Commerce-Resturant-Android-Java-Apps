package com.ds.androideatitv2client.Remote;

import com.ds.androideatitv2client.Model.FCMResponse;
import com.ds.androideatitv2client.Model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAKK2HWaQ:APA91bGDvCgZvaQFUB1kg17PWD5WCl7qlzBx2rh9-y8EylcI7IOE44qfIScmrZxZosLi1a_DzmXal0uXmsQexLyiBwLtHOG5ffvhIyJg1110uZsT-gdSrzO3PF71IEUNi7Ark6OY4a8I"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
