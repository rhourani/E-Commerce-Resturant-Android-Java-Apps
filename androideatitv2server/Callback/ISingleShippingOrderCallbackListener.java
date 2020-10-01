package com.ds.androideatitv2server.Callback;

import com.ds.androideatitv2server.Model.ShippingOrderModel;

public interface ISingleShippingOrderCallbackListener {
    void onSingleShippingOrderLoadSuccess(ShippingOrderModel shippingOrderModel);
}
