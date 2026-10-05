package com.vortexmakers.plateformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;

public class AudioManager {

    public static final String MENU_MUSIC = "Audios/Music/Pixel Dash.mp3";
    public static final String GAME_MUSIC = "Audios/Music/Rainbow Sprint.mp3";

    /** Delai (s) apres lequel la musique de menu reprend apres un jingle victoire / game over. */
    private static final float JINGLE_RESUME_DELAY = 3.5f;

    private static AudioManager instance;
    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    private Music music;
    private String currentMusicPath = "";
    private float musicVolume = 0.6f;
    private float sfxVolume   = 0.8f;
    private Timer.Task resumeMenuMusicTask;

    private Sound sndClick;
    private Sound sndRollover;
    private Sound sndSwitch;
    private Sound sndJump;
    private Sound sndCoin;
    private Sound sndLand;
    private Sound sndFall;
    private Sound sndWalking;
    private Sound sndBreak;
    private Sound sndVictory;
    private Sound sndGameOver;
    private Sound sndCheckpoint;
    private Sound sndEnemyHit;

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
        sndVictory    = loadSound("Audios/Platformer/victory.wav");
        sndGameOver   = loadSound("Audios/Platformer/game_over.wav");
        sndCheckpoint = loadSound("Audios/Platformer/checkpoint.wav");
        sndEnemyHit   = loadSound("Audios/Platformer/enemy_hit.wav");
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

    // ============================================================
    // MUSIQUE
    // ============================================================

    /** Musique des menus (Titre, Parametres, Lobby, ecrans de fin). Sans effet si deja en cours. */
    public void playMenuMusic() { playMusic(MENU_MUSIC); }

    /** Musique du niveau (GameScreen). Sans effet si deja en cours. */
    public void playGameMusic() { playMusic(GAME_MUSIC); }

    /** Compatibilite : musique par defaut = musique de menu. */
    public void playMusic() { playMenuMusic(); }

    public void playMusic(String path) {
        cancelResumeTask();
        if (currentMusicPath.equals(path) && music != null && music.isPlaying()) return;
        if (music != null) { music.stop(); music.dispose(); music = null; }
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

    private void cancelResumeTask() {
        if (resumeMenuMusicTask != null) { resumeMenuMusicTask.cancel(); resumeMenuMusicTask = null; }
    }

    /** Coupe la musique, joue un jingle, puis relance la musique de menu apres un court delai. */
    private void playJingle(Sound jingle, float vol) {
        cancelResumeTask();
        stopMusic();
        play(jingle, vol);
        resumeMenuMusicTask = Timer.schedule(new Timer.Task() {
            @Override public void run() {
                resumeMenuMusicTask = null;
                playMenuMusic();
            }
        }, JINGLE_RESUME_DELAY);
    }

    // ============================================================
    // SONS UI
    // ============================================================
    public void playClick()    { play(sndClick,    sfxVolume * 0.9f); }
    public void playRollover() { play(sndRollover, sfxVolume * 0.45f); }
    public void playSwitch()   { play(sndSwitch,   sfxVolume * 0.8f); }

    /** Ajoute uniquement le son de survol (hover) a un acteur. */
    public static void attachHoverSound(Actor actor) {
        actor.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1 && (fromActor == null || !fromActor.isDescendantOf(event.getListenerActor()))) {
                    AudioManager.getInstance().playRollover();
                }
            }
        });
    }

    /** Ajoute les sons de survol (hover) et de clic a un acteur (bouton). */
    public static void attachUiSounds(Actor actor) {
        attachHoverSound(actor);
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playClick();
            }
        });
    }

    // ============================================================
    // SONS GAMEPLAY
    // ============================================================
    public void playJump()  { play(sndJump,  sfxVolume); }
    public void playCoin()  { play(sndCoin,  sfxVolume); }
    public void playLand()  { play(sndLand,  sfxVolume * 0.8f); }
    public void playFall()  { play(sndFall,  sfxVolume * 0.9f); }
    public void playBreak() { play(sndBreak, sfxVolume * 0.7f); }
    public void playCheckpoint() { play(sndCheckpoint, sfxVolume); }
    public void playEnemyHit()   { play(sndEnemyHit,   sfxVolume * 0.9f); }

    // ============================================================
    // JINGLES DE FIN DE NIVEAU
    // ============================================================
    public void playVictory()  { playJingle(sndVictory,  sfxVolume); }
    public void playGameOver() { playJingle(sndGameOver, sfxVolume); }

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
        cancelResumeTask();
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
        if (sndVictory    != null) { sndVictory.dispose();    sndVictory    = null; }
        if (sndGameOver   != null) { sndGameOver.dispose();   sndGameOver   = null; }
        if (sndCheckpoint != null) { sndCheckpoint.dispose(); sndCheckpoint = null; }
        if (sndEnemyHit   != null) { sndEnemyHit.dispose();   sndEnemyHit   = null; }
        currentMusicPath = "";
        instance = null;
    }
}
