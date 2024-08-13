package com.example.audioboog.source;

import java.util.ArrayList;

public class Timeout {
    private static ArrayList<Timeout> timeouts;

    private int id;
    private int value;

    public Timeout(int id, int value) {
        this.id = id;
        this.value = value;
    }

    public static void initTimeouts() {
        timeouts = new ArrayList<Timeout>();
        for (int i = 1; i <= 30; i ++) {
            timeouts.add(new Timeout(i-1, i));
        }
    }

    public static ArrayList<Timeout> getTimeouts() {
        return timeouts;
    }

    public static String[] timeoutValues() {
        String[] timeoutValues = new String[timeouts.size()];
        for (int i = 0; i < timeouts.size(); i++) {
            timeoutValues[i] = timeouts.get(i).getPlaybackString();
        }
        return timeoutValues;
    }

    public int getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public String getPlaybackString() { return value + "min";}

    public static Timeout getByValue(float value) {
        for (int i = 0; i < timeouts.size(); i++) {
            Timeout timeout = timeouts.get(i);
            if (timeout.value == value) {
                return timeout;
            }
        }
        return null;
    }
}
