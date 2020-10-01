package com.ds.androideatitv2server.services;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.ds.androideatitv2server.Common.Common;
import com.ds.androideatitv2server.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFCMServices extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> dataRecv = remoteMessage.getData();
        if (dataRecv != null)
        {
            if (dataRecv.get(Common.NOTI_TITLE).equals("New Order"))
            {
                /*
                * Here we need to Mcall ainActivity cuz we must assign value for Common.currentUser
                * so we must call MainActivity to do that, if we directly called HomeActivity the will get crashing
                * due of Common.currentUser only be assigned in MainActivity AFTER LOGIN
                *  */

                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, true); // Using extra to detect if app opened from notification
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        intent);

            }
            else
            {
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        null);
            }

        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Common.updateToken(this,s,true, false);
    }
}
