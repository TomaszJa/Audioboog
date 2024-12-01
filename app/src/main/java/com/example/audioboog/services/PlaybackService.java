package com.example.audioboog.services;

import android.net.Uri;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.audioboog.source.Audiobook;
import com.example.audioboog.source.Chapter;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;

    @Override
    public void onCreate() {
        super.onCreate();
        androidx.media3.common.AudioAttributes playbackAttributes = new androidx.media3.common.AudioAttributes.Builder().
                setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        ExoPlayer exoPlayer = new ExoPlayer.Builder(this)
                .setAudioAttributes(playbackAttributes, true)
                .build();
        mediaSession = new MediaSession.Builder(this, exoPlayer).build();
    }

    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        // This example always accepts the connection request
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;
        super.onDestroy();
    }

    public void playAudiobook(Audiobook audiobook) {
        Player player = mediaSession.getPlayer();
        for (Chapter chapter: audiobook.getChapters()) {
            Uri uri = chapter.getPath();
            MediaItem item = MediaItem.fromUri(uri);
            player.addMediaItem(item);
        }
        player.prepare();
        player.play();
    }
}
