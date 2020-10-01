package com.ds.androideatitv2shipper.Callbacks;

import com.ds.androideatitv2shipper.Model.ShippingOrderModel;

import java.util.List;

public interface IShippingOrderCallbackListener {
    void onShippingOrderLoadSuccess(List<ShippingOrderModel> shippingOrderModelList);
    void onShippingOrderLoadFailed(String message);
}
