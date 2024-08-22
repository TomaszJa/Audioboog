package com.example.audioboog.services;

import static com.example.audioboog.services.ApplicationClass.ACTION_FORWARD;
import static com.example.audioboog.services.ApplicationClass.ACTION_PLAY;
import static com.example.audioboog.services.ApplicationClass.ACTION_REVERT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent1 = new Intent(context, MediaPlayerService.class);
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_FORWARD:
                case ACTION_REVERT:
                case ACTION_PLAY:
                    intent1.putExtra("myActionName", intent.getAction());
                    context.startService(intent1);
                    break;
            }
        }
    }
}
