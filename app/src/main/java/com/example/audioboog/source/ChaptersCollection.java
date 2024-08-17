package com.example.audioboog.source;

import java.util.ArrayList;

public class ChaptersCollection {
    private static ArrayList<ChaptersCollection> chaptersCollections;

    private int id;
    private Chapter chapter;

    public ChaptersCollection(int id, Chapter chapter) {
        this.id = id;
        this.chapter = chapter;
    }

    public static void initChaptersCollection(ArrayList<Chapter> chapters) {
        chaptersCollections = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            chaptersCollections.add(new ChaptersCollection(i, chapters.get(i)));
        }
    }

    public static ArrayList<ChaptersCollection> getChaptersCollections() {
        return chaptersCollections;
    }

    public int getId() {
        return id;
    }

    public static String[] chaptersNumbers() {
        String[] chaptersNames = new String[chaptersCollections.size()];
        for (int i = 0; i < chaptersCollections.size(); i++) {
            chaptersNames[i] = String.valueOf(chaptersCollections.get(i).chapter.getChapterNumber());
        }
        return chaptersNames;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public static ChaptersCollection getByUid(String uid) {
        for (int i = 0; i < chaptersCollections.size(); i++) {
            ChaptersCollection chapter = chaptersCollections.get(i);
            if (chapter.getChapter().getUid().equals(uid)) {
                return chapter;
            }
        }
        return null;
    }
}

