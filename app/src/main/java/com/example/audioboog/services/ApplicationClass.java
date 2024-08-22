package com.example.audioboog.services;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class ApplicationClass extends Application {
    public static final String CHANNEL_ID_1 = "CHANNEL_1";
    public static final String ACTION_FORWARD = "FORWARD";
    public static final String ACTION_REVERT = "REVERT";
    public static final String ACTION_PLAY = "PLAY";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel notificationChannel1 = new NotificationChannel(CHANNEL_ID_1, "Channel(1)", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel1.setDescription("Channel 1 Description");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel1);
    }
}
