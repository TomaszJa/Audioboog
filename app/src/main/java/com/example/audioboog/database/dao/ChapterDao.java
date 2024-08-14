package com.example.audioboog.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.audioboog.source.Chapter;

import java.util.List;

@Dao
public interface ChapterDao {
    @Query("SELECT * FROM chapter")
    List<Chapter> getAll();

    @Query("SELECT * FROM chapter WHERE uid LIKE :uid")
    Chapter getChapterById(String uid);

    @Query("SELECT * FROM chapter WHERE uid IN (:chapterIds)")
    List<Chapter> loadAllByIds(String[] chapterIds);

    @Query("SELECT * FROM chapter WHERE book_name LIKE :bookName")
    List<Chapter> loadAllByBookName(String bookName);

//    @Query("SELECT * FROM chapter WHERE name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    Chapter findByName(String first, String last);

    @Update
    void updateChapter(Chapter chapter);

    @Insert
    void insertAll(Chapter... chapters);

    @Delete
    void delete(Chapter chapter);
}
