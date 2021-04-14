package com.stegano.cameramedia;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;

public class PlayActivity extends AppCompatActivity {
    private VideoView videoView;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        videoView = (VideoView) findViewById(R.id.videoView);

        mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.start();
                mediaController.show();
            }
        });

        videoView.setVideoPath(getOutputMediaFile().getAbsolutePath());
    }

    private File getOutputMediaFile() {
        String recordPath = getExternalCacheDir().getAbsolutePath();
        File mediaFile = new File(recordPath + File.separator + "record.mp4");
        return mediaFile;
    }
}