package com.ds.androideatitv2client.Callback;

import com.ds.androideatitv2client.Model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSuccess(OrderModel orderModel, long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
