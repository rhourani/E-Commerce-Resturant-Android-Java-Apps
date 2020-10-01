package com.ds.androideatitv2shipper.Services;

import androidx.annotation.NonNull;


import com.ds.androideatitv2shipper.Common.Common;
import com.ds.androideatitv2shipper.Model.EventBus.UpdateShippingOrderEvent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.Random;

public class MyFCMServices extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> dataRecv = remoteMessage.getData();
        if (dataRecv != null)
        {
            Common.showNotification(this, new Random().nextInt(),
                    dataRecv.get(Common.NOTI_TITLE),
                    dataRecv.get(Common.NOTI_CONTENT),
                    null);
            EventBus.getDefault().postSticky(new UpdateShippingOrderEvent()); // Update order list when have new order needs ship
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Common.updateToken(this,s, false, true); // We are in Shipper App
    }
}
