package com.example.audioboog.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.audioboog.database.dao.AudiobookDao;
import com.example.audioboog.database.dao.ChapterDao;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;

@Database(entities = {Chapter.class, Audiobook.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChapterDao chapterDao();
    public abstract AudiobookDao audiobookDao();
}