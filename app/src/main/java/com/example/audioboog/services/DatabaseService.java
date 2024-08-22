package com.example.audioboog.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.room.Room;

import com.example.audioboog.database.AppDatabase;
import com.example.audioboog.database.dao.AudiobookDao;
import com.example.audioboog.database.dao.ChapterDao;
import com.example.audioboog.database.relationships.AudiobookWithChapters;
import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseService extends Service {
    private final IBinder binder = new DatabaseService.LocalBinder();
    AppDatabase db;
    AudiobookDao audiobookDao;
    ChapterDao chapterDao;
    ExecutorService executorService;

    public class LocalBinder extends Binder {
        public DatabaseService getService() {
            return DatabaseService.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "database-name")
                .enableMultiInstanceInvalidation()
                .fallbackToDestructiveMigration()
                .build();
        audiobookDao = db.audiobookDao();
        chapterDao = db.chapterDao();
        executorService = Executors.newFixedThreadPool(4);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    public ArrayList<Audiobook> getAudiobooks() {
        return submitTask(this::loadAudiobooksFromDatabase);
    }

    public void updateAudiobook(Audiobook audiobook) {
        submitTask(() -> updateAudiobookInDatabase(audiobook));
    }

    public void deleteAudiobook(Audiobook audiobook) {
        submitTask(() -> deleteAudiobookInDatabase(audiobook));
    }

    private void submitTask(Runnable task) {
        try {
            executorService.submit(task).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T submitTask(Callable<T> callable) {
        try {
            return executorService.submit(callable).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<Audiobook> loadAudiobooksFromDatabase() {
        ArrayList<Audiobook> audiobooks = new ArrayList<>();
        AudiobookDao audiobookDao = db.audiobookDao();
        List<AudiobookWithChapters> audiobooksWithChapters = audiobookDao.getAll();

        for (AudiobookWithChapters audiobookWithChapter : audiobooksWithChapters) {
            Audiobook audiobook = audiobookWithChapter.audiobook;
            audiobook.updateWithChapters(new ArrayList<>(audiobookWithChapter.chapters));
            audiobooks.add(audiobook);
        }
        return audiobooks;
    }

    public void updateAudiobookInDatabase(Audiobook audiobook) {
        if (audiobookDao.getAudiobookById(audiobook.getUid()) == null) {
            audiobookDao.insertAll(audiobook);
        } else {
            audiobookDao.updateAudiobook(audiobook);
        }
        updateChaptersForAudiobook(audiobook);
    }

    private void deleteAudiobookInDatabase(Audiobook audiobook) {
        audiobookDao.delete(audiobook);
    }

    private void updateChaptersForAudiobook(Audiobook audiobook) {
        for (Chapter chapter : audiobook.getChapters()) {
            updateChapter(chapter);
        }
    }

    private void updateChapter(Chapter chapter) {
        if (chapterDao.getChapterById(chapter.getUid()) != null) {
            chapterDao.updateChapter(chapter);
        }
        chapterDao.insertAll(chapter);
    }
}
