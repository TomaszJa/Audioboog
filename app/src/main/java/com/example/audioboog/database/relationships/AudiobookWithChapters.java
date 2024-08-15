package com.example.audioboog.database.relationships;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;

import java.util.List;

public class AudiobookWithChapters {
    @Embedded
    public Audiobook audiobook;
    @Relation(parentColumn = "uid", entityColumn = "audiobook_uid")
    public List<Chapter> chapters;
}
