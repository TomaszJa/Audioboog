package com.example.audioboog.source;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.audioboog.database.converters.ByteArrayConverter;
import com.example.audioboog.database.converters.UriConverter;

import java.util.Objects;
import java.util.UUID;

@Entity(foreignKeys = {@ForeignKey(entity = Audiobook.class,
        parentColumns = "uid",
        childColumns = "audiobook_uid",
        onDelete = ForeignKey.CASCADE)
})
public class Chapter implements Comparable<Chapter>, Parcelable {

    @PrimaryKey
    @NonNull
    private String uid;
    @ColumnInfo(name = "audiobook_uid")
    private String audiobookUid;
    @ColumnInfo(name = "chapter_number")
    private int chapterNumber;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "book_name")
    private String bookName;
    @ColumnInfo(name = "path")
    @TypeConverters(UriConverter.class)
    private Uri path;
    @ColumnInfo(name = "embedded_picture")
    @TypeConverters(ByteArrayConverter.class)
    private byte[] embeddedPicture;
    @ColumnInfo(name = "current_position")
    private long currentPosition;
    @ColumnInfo(name = "total_duration")
    private long totalDuration;
    @ColumnInfo(name = "chapter_start")
    private long chapterStart;

    @Ignore
    public Chapter(@NonNull String audiobookUid, int chapterNumber, String name, String bookName, Uri path, byte[] embeddedPicture, long currentPosition, long totalDuration) {
        this.audiobookUid = audiobookUid;
        uid = UUID.randomUUID().toString();
        this.chapterNumber = chapterNumber;
        this.name = name;
        this.bookName = bookName;
        this.path = path;
        this.embeddedPicture = embeddedPicture;
        this.currentPosition = currentPosition;
        this.totalDuration = totalDuration;
        this.chapterStart = 0;
    }

    public Chapter(@NonNull String uid, @NonNull String audiobookUid, int chapterNumber, String name, String bookName, Uri path, byte[] embeddedPicture, long currentPosition, long totalDuration, long chapterStart) {
        this.uid = uid;
        this.audiobookUid = audiobookUid;
        this.chapterNumber = chapterNumber;
        this.name = name;
        this.bookName = bookName;
        this.path = path;
        this.embeddedPicture = embeddedPicture;
        this.currentPosition = currentPosition;
        this.totalDuration = totalDuration;
        this.chapterStart = chapterStart;
    }

    protected Chapter(Parcel in) {
        uid = Objects.requireNonNull(in.readString());
        audiobookUid = Objects.requireNonNull(in.readString());
        chapterNumber = in.readInt();
        name = in.readString();
        bookName = in.readString();
        path = in.readParcelable(Uri.class.getClassLoader());
        embeddedPicture = in.readBlob();
        currentPosition = in.readLong();
        totalDuration = in.readLong();
        chapterStart = in.readLong();
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

    @NonNull
    public String getUid() {
        return uid;
    }

    public String getAudiobookUid() {
        return audiobookUid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public long getChapterStart() {
        return chapterStart;
    }

    public long getChapterEnd() {
        return getChapterStart() + getTotalDuration();
    }

    public void setChapterStart(long chapterStart) {
        this.chapterStart = chapterStart;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public void setPath(Uri path) {
        this.path = path;
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
        dest.writeString(audiobookUid);
        dest.writeInt(chapterNumber);
        dest.writeString(name);
        dest.writeString(bookName);
        dest.writeParcelable(path, flags);
        dest.writeBlob(embeddedPicture);
        dest.writeLong(currentPosition);
        dest.writeLong(totalDuration);
        dest.writeLong(chapterStart);
    }
}
