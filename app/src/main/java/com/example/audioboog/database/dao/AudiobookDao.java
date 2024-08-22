package com.example.audioboog.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.audioboog.database.relationships.AudiobookWithChapters;
import com.example.audioboog.source.Audiobook;

import java.util.List;

@Dao
public interface AudiobookDao {
    @Query("SELECT * FROM audiobook")
    List<AudiobookWithChapters> getAll();

    @Transaction
    @Query("SELECT * FROM audiobook WHERE uid = :uid")
    AudiobookWithChapters getAudiobookById(String uid);

    @Update
    void updateAudiobook(Audiobook audiobook);

    @Insert
    void insertAll(Audiobook... audiobooks);

    @Delete
    void delete(Audiobook audiobook);
}
