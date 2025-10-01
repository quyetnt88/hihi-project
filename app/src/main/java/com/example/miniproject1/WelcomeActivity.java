package com.example.miniproject1;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class WelcomeActivity extends AppCompatActivity {
    private Button btnStart;
    private MediaPlayer bgm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        bgm = android.media.MediaPlayer.create(this, R.raw.bg_welcome);
        bgm.setLooping(true);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (bgm != null && !bgm.isPlaying()) bgm.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bgm != null && bgm.isPlaying()) bgm.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bgm != null) {
            bgm.release();
            bgm = null;
        }
    }
}