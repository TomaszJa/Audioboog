package com.example.audioboog.source;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.audioboog.database.converters.ByteArrayConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Audiobook implements Parcelable {
    @PrimaryKey
    @NonNull
    private String uid;
    @ColumnInfo(name = "name")
    private String name;
    @Ignore
    private ArrayList<Chapter> chapters;
    @ColumnInfo(name = "current_chapter")
    private int currentChapterNumber;
    @ColumnInfo(name = "embedded_picture")
    @TypeConverters(ByteArrayConverter.class)
    private byte[] embeddedPicture;
    @ColumnInfo(name = "current_position")
    private long currentPosition;
    @ColumnInfo(name = "total_duration")
    private long totalDuration;

    public Audiobook() {
        uid = UUID.randomUUID().toString();
        this.name = "";
        this.chapters = new ArrayList<>();
        this.currentPosition = 0;
        this.totalDuration = 0;
        this.currentChapterNumber = 0;
        this.embeddedPicture = null;
    }

    public Audiobook(String name, ArrayList<Chapter> chapters, long currentPosition) {
        uid = UUID.randomUUID().toString();
        this.name = name;
        this.chapters = chapters;
        this.currentPosition = currentPosition;
        for (Chapter chapter: chapters) {
            this.totalDuration += chapter.getTotalDuration();
        }
        this.currentChapterNumber = 0;
        this.embeddedPicture = chapters.get(0).getEmbeddedPicture();
    }

    public Audiobook(String name, ArrayList<Chapter> chapters, int currentChapterNumber, int currentPosition) {
        uid = UUID.randomUUID().toString();
        this.name = name;
        this.chapters = chapters;
        this.currentPosition = currentPosition;
        for (Chapter chapter: chapters) {
            this.totalDuration += chapter.getTotalDuration();
        }
        this.currentChapterNumber = currentChapterNumber;
        this.embeddedPicture = chapters.get(0).getEmbeddedPicture();
    }

    protected Audiobook(Parcel in) {
        uid = Objects.requireNonNull(in.readString());
        name = in.readString();
        chapters = in.createTypedArrayList(Chapter.CREATOR);
        currentChapterNumber = in.readInt();
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

    public void updateWithChapters(ArrayList<Chapter> chapters) {
        Collections.sort(chapters);
        setChapters(chapters);
        setName(chapters.get(0).getBookName());
        setEmbeddedPicture(chapters.get(0).getEmbeddedPicture());
        this.totalDuration = 0;
        for (Chapter chapter: this.chapters) {
            chapter.setChapterStart(this.totalDuration);
            this.totalDuration += chapter.getTotalDuration();
        }
    }

    public void setChapterByPosition(int position) {
        for (int i = 0; i < chapters.size(); i++) {
            if (position > chapters.get(i).getChapterStart() && position < (chapters.get(i).getChapterStart() + chapters.get(i).getTotalDuration())) {
                setCurrentChapterNumber(i);
            }
        }
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

    public int getCurrentChapterNumber() {
        return currentChapterNumber;
    }

    public Chapter getCurrentChapter() {
        return chapters.get(currentChapterNumber);
    }

    public void setPreviousChapterAsCurrent() {
        currentChapterNumber--;
    }

    public void setNextChapterAsCurrent() {
        currentChapterNumber++;
    }

    public Chapter getPreviousChapter() {
        if (currentChapterNumber > 0) {
            return chapters.get(currentChapterNumber - 1);
        }
        return null;
    }

    public Chapter getNextChapter() {
        if (currentChapterNumber < chapters.size() - 1) {
            return chapters.get(currentChapterNumber + 1);
        }
        return null;
    }

    public void setCurrentChapterNumber(int currentChapterNumber) {
        this.currentChapterNumber = currentChapterNumber;
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

    public void setChapters(ArrayList<Chapter> chapters) {
        this.chapters = chapters;
    }

    public void setEmbeddedPicture(byte[] embeddedPicture) {
        this.embeddedPicture = embeddedPicture;
    }

    public void setCurrentPosition(long currentPosition) {
        this.currentPosition = currentPosition;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
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
        dest.writeInt(currentChapterNumber);
        dest.writeBlob(embeddedPicture);
        dest.writeLong(currentPosition);
        dest.writeLong(totalDuration);
    }
}
