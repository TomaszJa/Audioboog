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
    private long currentPosition;
    private long totalDuration;

    public Chapter(int chapterNumber, String name, String bookName, Uri path, byte[] embeddedPicture, long currentPosition, long totalDuration) {
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
        currentPosition = in.readLong();
        totalDuration = in.readLong();
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

    public long getCurrentPosition() {
        return currentPosition;
    }

    public long getTotalDuration() {
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
        dest.writeLong(currentPosition);
        dest.writeLong(totalDuration);
    }
}
