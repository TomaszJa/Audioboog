package com.example.audioboog.source;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.UUID;

public class Chapter implements Comparable<Chapter>, Parcelable {
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

    protected Chapter(Parcel in) {
        uid = in.readString();
        chapterNumber = in.readInt();
        name = in.readString();
        bookName = in.readString();
        path = in.readParcelable(Uri.class.getClassLoader());
        embeddedPicture = in.readBlob();
        currentPosition = in.readInt();
        totalDuration = in.readInt();
    }

    public static final Creator<Chapter> CREATOR = new Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel in) {
            return new Chapter(in);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeInt(chapterNumber);
        dest.writeString(name);
        dest.writeString(bookName);
        dest.writeParcelable(path, flags);
        dest.writeBlob(embeddedPicture);
        dest.writeInt(currentPosition);
        dest.writeInt(totalDuration);
    }
}
