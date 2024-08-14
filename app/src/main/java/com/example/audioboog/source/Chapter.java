package com.example.audioboog.source;

import android.net.Uri;

import java.util.UUID;

public class Chapter implements Comparable<Chapter> {
    private String uid;
    private int chapterNumber;
    private String name;
    private String bookName;
    private Uri path;
    private byte[] embeddedPicture;
    private int currentPosition;
    private int totalDuration;

    public Chapter(int chapterNumber, String name, String bookName, Uri path, byte[] embeddedPicture, int currentPosition, int totalDuration) {
        uid = UUID.randomUUID().toString();
        this.chapterNumber = chapterNumber;
        this.name = name;
        this.bookName = bookName;
        this.path = path;
        this.embeddedPicture = embeddedPicture;
        this.currentPosition = currentPosition;
        this.totalDuration = totalDuration;
    }

    public String getUid() {
        return uid;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getName() {
        return name;
    }

    public Uri getPath() {
        return path;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public String getBookName() {
        return bookName;
    }

    public byte[] getEmbeddedPicture() {
        return embeddedPicture;
    }

    @Override
    public int compareTo(Chapter chapter) {
        int compare = chapter.getChapterNumber();
        return this.getChapterNumber() - compare;
    }
}
