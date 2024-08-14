package com.example.audioboog.source;

import android.net.Uri;

import java.util.ArrayList;
import java.util.UUID;

public class Audiobook {
    private String uid;
    private String name;
    private ArrayList<Chapter> chapters;
    private int currentChapter;
    private byte[] embeddedPicture;
    private int currentPosition;
    private int totalDuration;

    public Audiobook(String name, ArrayList<Chapter> chapters, int currentPosition) {
        uid = UUID.randomUUID().toString();
        this.name = name;
        this.chapters = chapters;
        this.currentPosition = currentPosition;
        for (Chapter chapter: chapters) {
            this.totalDuration += chapter.getTotalDuration();
        }
        this.currentChapter = 0;
        this.embeddedPicture = chapters.get(0).getEmbeddedPicture();
    }

    public Audiobook(String name, ArrayList<Chapter> chapters, int currentChapter, int currentPosition) {
        uid = UUID.randomUUID().toString();
        this.name = name;
        this.chapters = chapters;
        this.currentPosition = currentPosition;
        for (Chapter chapter: chapters) {
            this.totalDuration += chapter.getTotalDuration();
        }
        this.currentChapter = currentChapter;
        this.embeddedPicture = chapters.get(0).getEmbeddedPicture();
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Chapter> getChapters() {
        return chapters;
    }

    public Chapter getCurrentChapter() {
        return chapters.get(currentChapter);
    }

    public byte[] getEmbeddedPicture() {
        return embeddedPicture;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getTotalDuration() {
        return totalDuration;
    }
}
