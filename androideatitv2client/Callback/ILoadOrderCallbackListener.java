package com.ds.androideatitv2client.Callback;

import com.ds.androideatitv2client.Model.OrderModel;

import java.util.List;

public interface ILoadOrderCallbackListener {
    void onLoadOrderSuccess(List<OrderModel> orderModelList);
    void onLoadOrderFailed(String message);

}
