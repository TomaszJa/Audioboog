package com.example.audioboog.source;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.UUID;

@Entity
public class Audiobook implements Parcelable {
    @PrimaryKey
    private String uid;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "chapters")
    private ArrayList<Chapter> chapters;
    @ColumnInfo(name = "current_chapter")
    private int currentChapter;
    @ColumnInfo(name = "embedded_picture")
    private byte[] embeddedPicture;
    @ColumnInfo(name = "current_position")
    private long currentPosition;
    @ColumnInfo(name = "total_duration")
    private long totalDuration;

    public Audiobook(String name, ArrayList<Chapter> chapters, long currentPosition) {
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
        currentPosition = in.readLong();
        totalDuration = in.readLong();
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

    public void setPreviousChapterAsCurrent() {
        currentChapter--;
    }

    public void setNextChapterAsCurrent() {
        currentChapter++;
    }

    public Chapter getPreviousChapter() {
        if (currentChapter > 0) {
            return chapters.get(currentChapter - 1);
        }
        return null;
    }

    public Chapter getNextChapter() {
        if (currentChapter < chapters.size() - 1) {
            return chapters.get(currentChapter + 1);
        }
        return null;
    }

    public void setCurrentChapter(int currentChapter) {
        this.currentChapter = currentChapter;
    }

    public byte[] getEmbeddedPicture() {
        return embeddedPicture;
    }

    public long getCurrentPosition() {
        return currentPosition;
    }

    public long getTotalDuration() {
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
        dest.writeLong(currentPosition);
        dest.writeLong(totalDuration);
    }
}
