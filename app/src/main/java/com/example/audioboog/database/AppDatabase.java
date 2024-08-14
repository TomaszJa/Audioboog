package com.example.audioboog.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.audioboog.database.dao.ChapterDao;
import com.example.audioboog.source.Chapter;

@Database(entities = {Chapter.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChapterDao chapterDao();
}