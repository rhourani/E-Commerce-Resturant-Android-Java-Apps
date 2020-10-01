package com.ds.androideatitv2server.Callback;

import com.ds.androideatitv2server.Model.OrderModel;

import java.util.List;

public interface IOrderCallbackListener {
    void onOrderLoadSuccess(List<OrderModel> orderModelList);
    void onOrderLoadFailed(String message);
}
