package com.example.audioboog.source;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

public class Audiobook implements Parcelable {
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

    protected Audiobook(Parcel in) {
        uid = in.readString();
        name = in.readString();
        chapters = in.createTypedArrayList(Chapter.CREATOR);
        currentChapter = in.readInt();
        embeddedPicture = in.readBlob();
        currentPosition = in.readInt();
        totalDuration = in.readInt();
    }

    public static final Creator<Audiobook> CREATOR = new Creator<Audiobook>() {
        @Override
        public Audiobook createFromParcel(Parcel in) {
            return new Audiobook(in);
        }

        @Override
        public Audiobook[] newArray(int size) {
            return new Audiobook[size];
        }
    };

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

    public Chapter getPreviousChapter() {
        if (currentChapter > 0) {
            currentChapter--;
            return chapters.get(currentChapter);
        }
        return null;
    }

    public Chapter getNextChapter() {
        if (currentChapter < chapters.size() - 1) {
            currentChapter++;
            return chapters.get(currentChapter);
        }
        return null;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(name);
        dest.writeTypedList(chapters);
        dest.writeInt(currentChapter);
        dest.writeBlob(embeddedPicture);
        dest.writeInt(currentPosition);
        dest.writeInt(totalDuration);
    }
}
