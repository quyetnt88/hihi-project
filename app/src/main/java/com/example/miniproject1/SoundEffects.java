package com.example.miniproject1;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.HashMap;

public class SoundEffects {
    private SoundPool soundPool;
    private final HashMap<Integer, Integer> sounds = new HashMap<>();
    private boolean loaded = false;

    public SoundEffects() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> loaded = true);
    }

    public int load(Context ctx, int resId) {
        int id = soundPool.load(ctx, resId, 1);
        sounds.put(resId, id);
        return id;
    }

    public void play(int resId, float volume) {
        if (!loaded || !sounds.containsKey(resId)) return;
        soundPool.play(sounds.get(resId), volume, volume, 1, 0, 1f);
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
