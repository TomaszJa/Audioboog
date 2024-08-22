package com.example.audioboog.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.audioboog.source.Chapter;

@Dao
public interface ChapterDao {
    @Query("SELECT * FROM chapter WHERE uid LIKE :uid")
    Chapter getChapterById(String uid);

    @Update
    void updateChapter(Chapter chapter);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Chapter... chapters);

    @Delete
    void delete(Chapter chapter);
}
