package com.example.audioboog.source;

import java.util.ArrayList;

public class PlaybackSpeed {
    private static ArrayList<PlaybackSpeed> playbackSpeeds;

    private int id;
    private float value;

    public PlaybackSpeed(int id, float value) {
        this.id = id;
        this.value = value;
    }

    public static void initPlaybackSpeeds() {
        playbackSpeeds = new ArrayList<PlaybackSpeed>();
        for (int i = 50; i <= 200; i += 5) {
            float speed = (float) Math.round((float)i)/100;
            playbackSpeeds.add(new PlaybackSpeed((i-50)/5, speed));
        }
    }

    public static ArrayList<PlaybackSpeed> getPlaybackSpeeds() {
        return playbackSpeeds;
    }

    public static String[] playbackValues() {
        String[] playbackValues = new String[playbackSpeeds.size()];
        for (int i = 0; i < playbackSpeeds.size(); i++) {
            playbackValues[i] = playbackSpeeds.get(i).getPlaybackString();
        }
        return playbackValues;
    }

    public int getId() {
        return id;
    }

    public float getValue() {
        return value;
    }

    public String getPlaybackString() { return String.format("%.2f", value) + "x";}

    public static PlaybackSpeed getByValue(float value) {
        for (int i = 0; i < playbackSpeeds.size(); i++) {
            PlaybackSpeed playbackSpeed = playbackSpeeds.get(i);
            if (playbackSpeed.value == value) {
                return playbackSpeed;
            }
        }
        return null;
    }
}
