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

    @Query("SELECT * FROM audiobook WHERE uid IN (:audiobooksIds)")
    List<Audiobook> loadAllByIds(String[] audiobooksIds);

    @Query("SELECT * FROM audiobook WHERE name LIKE :bookName LIMIT 1")
    Audiobook loadByBookName(String bookName);

//    @Query("SELECT * FROM chapter WHERE name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    Chapter findByName(String first, String last);

    @Update
    void updateAudiobook(Audiobook audiobook);

    @Insert
    void insertAll(Audiobook... audiobooks);

    @Delete
    void delete(Audiobook audiobook);
}
