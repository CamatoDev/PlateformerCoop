package com.vortexmakers.plateformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class AudioManager {

    private static AudioManager instance;
    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    private Music music;
    private String currentMusicPath = "";
    private float musicVolume = 0.6f;
    private float sfxVolume   = 0.8f;

    private Sound sndClick;
    private Sound sndRollover;
    private Sound sndSwitch;
    private Sound sndJump;
    private Sound sndCoin;
    private Sound sndLand;
    private Sound sndFall;
    private Sound sndWalking;
    private Sound sndBreak;

    private float walkTimer = 0f;
    private static final float WALK_INTERVAL = 0.35f;

    private AudioManager() {}

    public void load() {
        sndClick    = loadSound("Audios/UI/click1.wav");
        sndRollover = loadSound("Audios/UI/rollover1.wav");
        sndSwitch   = loadSound("Audios/UI/switch14.wav");
        sndJump    = loadSound("Audios/Platformer/jump.ogg");
        sndCoin    = loadSound("Audios/Platformer/coin.ogg");
        sndLand    = loadSound("Audios/Platformer/land.ogg");
        sndFall    = loadSound("Audios/Platformer/fall.ogg");
        sndWalking = loadSound("Audios/Platformer/walking.ogg");
        sndBreak   = loadSound("Audios/Platformer/break.ogg");
        System.out.println("[AudioManager] Sons charges");
    }

    private Sound loadSound(String path) {
        try {
            if (Gdx.files.internal(path).exists())
                return Gdx.audio.newSound(Gdx.files.internal(path));
        } catch (Exception e) {
            System.out.println("[AudioManager] Son introuvable : " + path);
        }
        return null;
    }

    public void playMusic() { playMusic("Audios/Music/Pixel Dash.mp3"); }

    public void playMusic(String path) {
        if (currentMusicPath.equals(path) && music != null && music.isPlaying()) return;
        if (music != null) { music.stop(); music.dispose(); }
        try {
            if (Gdx.files.internal(path).exists()) {
                music = Gdx.audio.newMusic(Gdx.files.internal(path));
                music.setLooping(true);
                music.setVolume(musicVolume);
                music.play();
                currentMusicPath = path;
                System.out.println("[AudioManager] Musique : " + path);
            }
        } catch (Exception e) {
            System.out.println("[AudioManager] Erreur musique : " + e.getMessage());
        }
    }

    public void pauseMusic()  { if (music != null) music.pause(); }
    public void resumeMusic() { if (music != null) music.play(); }
    public void stopMusic()   { if (music != null) { music.stop(); currentMusicPath = ""; } }

    public void setMusicVolume(float vol) {
        musicVolume = Math.max(0f, Math.min(1f, vol));
        if (music != null) music.setVolume(musicVolume);
    }
    public float getMusicVolume() { return musicVolume; }
    public boolean isMusicPlaying() { return music != null && music.isPlaying(); }

    public void playClick()    { play(sndClick,    sfxVolume * 0.9f); }
    public void playRollover() { play(sndRollover, sfxVolume * 0.45f); }
    public void playSwitch()   { play(sndSwitch,   sfxVolume * 0.8f); }
    public void playJump()  { play(sndJump,  sfxVolume); }
    public void playCoin()  { play(sndCoin,  sfxVolume); }
    public void playLand()  { play(sndLand,  sfxVolume * 0.8f); }
    public void playFall()  { play(sndFall,  sfxVolume * 0.9f); }
    public void playBreak() { play(sndBreak, sfxVolume * 0.7f); }

    public void playWalking(float delta) {
        if (sndWalking == null) return;
        walkTimer += delta;
        if (walkTimer >= WALK_INTERVAL) {
            walkTimer = 0f;
            sndWalking.play(sfxVolume * 0.5f);
        }
    }
    public void resetWalkTimer() { walkTimer = WALK_INTERVAL; }

    public void setSfxVolume(float vol) { sfxVolume = Math.max(0f, Math.min(1f, vol)); }
    public float getSfxVolume() { return sfxVolume; }

    private void play(Sound sound, float vol) {
        if (sound != null) sound.play(Math.max(0f, Math.min(1f, vol)));
    }

    public void dispose() {
        if (music       != null) { music.stop(); music.dispose(); music = null; }
        if (sndClick    != null) { sndClick.dispose();    sndClick    = null; }
        if (sndRollover != null) { sndRollover.dispose(); sndRollover = null; }
        if (sndSwitch   != null) { sndSwitch.dispose();   sndSwitch   = null; }
        if (sndJump     != null) { sndJump.dispose();     sndJump     = null; }
        if (sndCoin     != null) { sndCoin.dispose();     sndCoin     = null; }
        if (sndLand     != null) { sndLand.dispose();     sndLand     = null; }
        if (sndFall     != null) { sndFall.dispose();     sndFall     = null; }
        if (sndWalking  != null) { sndWalking.dispose();  sndWalking  = null; }
        if (sndBreak    != null) { sndBreak.dispose();    sndBreak    = null; }
        currentMusicPath = "";
        instance = null;
    }
}
