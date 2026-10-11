package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.vortexmakers.plateformer.utils.AudioManager;
import com.vortexmakers.plateformer.utils.FontManager;

/**
 * PAUSEMENU - Panneau de pause affiche par-dessus le jeu.
 *
 * ATTENTION : le jeu est multijoueur et continue de tourner pour les autres joueurs.
 * Ce menu ne met donc PAS le jeu en pause : il bloque uniquement les controles du joueur local.
 *
 * Contenu : volume musique, volume SFX, plein ecran, rappel des controles,
 * bouton "Reprendre", bouton "Quitter la partie" (avec confirmation).
 *
 * Les polices viennent de FontManager (partagees) : on n'appelle JAMAIS getData().setScale().
 */
public class PauseMenu {

    /** Evenements remontes vers le GameScreen. */
    public interface Callbacks {
        void onResume();
        void onQuitConfirmed();
    }

    private static final int PANEL_W = 600, PANEL_H = 560;
    private static final int CONFIRM_W = 540, CONFIRM_H = 320;
    private static final int BTN_W = 300, BTN_H = 56;
    private static final int SLIDER_W = 250;

    private final Callbacks callbacks;
    private final boolean isHost;

    private Stage stage;
    private final Array<Texture> textures = new Array<>();

    private Image dim;
    private Table confirmLayer;
    private boolean confirmVisible = false;

    private Slider musicSlider, sfxSlider;
    private Label musicValue, sfxValue;
    private TextButton fullscreenBtn;
    private boolean syncing = false;

    public PauseMenu(Callbacks callbacks, boolean isHost) {
        this.callbacks = callbacks;
        this.isHost = isHost;
        build();
    }

    // ============================================================
    // OUTILS GRAPHIQUES
    // ============================================================
    private Texture roundRect(int w, int h, int r, Color c) {
        Pixmap px = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        px.setColor(c);
        r = Math.min(r, Math.min(w, h) / 2);
        px.fillRectangle(r, 0, w - 2 * r, h);
        px.fillRectangle(0, r, w, h - 2 * r);
        px.fillCircle(r, r, r);
        px.fillCircle(w - r, r, r);
        px.fillCircle(r, h - r, r);
        px.fillCircle(w - r, h - r, r);
        Texture t = new Texture(px);
        px.dispose();
        textures.add(t);
        return t;
    }

    private Texture circle(int d, Color c) {
        Pixmap px = new Pixmap(d, d, Pixmap.Format.RGBA8888);
        px.setColor(c);
        px.fillCircle(d / 2, d / 2, d / 2 - 1);
        Texture t = new Texture(px);
        px.dispose();
        textures.add(t);
        return t;
    }

    private Texture solid(Color c) {
        Pixmap px = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        px.setColor(c);
        px.fill();
        Texture t = new Texture(px);
        px.dispose();
        textures.add(t);
        return t;
    }

    private TextureRegionDrawable drawable(Texture t) {
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    private TextButton button(String text, BitmapFont font, int w, int h, Color up, Color over) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font;
        s.fontColor = Color.WHITE;
        s.up   = drawable(roundRect(w, h, 26, up));
        s.over = drawable(roundRect(w, h, 26, over));
        s.down = drawable(roundRect(w, h, 26, new Color(up.r * 0.85f, up.g * 0.85f, up.b * 0.85f, 1f)));
        TextButton b = new TextButton(text, s);
        AudioManager.attachUiSounds(b);
        return b;
    }

    // ============================================================
    // CONSTRUCTION
    // ============================================================
    private void build() {
        stage = new Stage(new ExtendViewport(1080, 720));

        FontManager fm = FontManager.getInstance();
        BitmapFont titleFont = fm.getSubtitle();
        BitmapFont bodyFont  = fm.getBody();
        BitmapFont smallFont = fm.getSmall();

        final AudioManager audio = AudioManager.getInstance();

        // Voile sombre derriere le panneau
        dim = new Image(drawable(solid(new Color(0.05f, 0.03f, 0.14f, 0.62f))));
        dim.setSize(1080, 720);
        stage.addActor(dim);

        // ---------- PANNEAU PRINCIPAL ----------
        Table panel = new Table();
        panel.setBackground(drawable(roundRect(PANEL_W, PANEL_H, 36, new Color(0.60f, 0.55f, 0.90f, 0.97f))));
        panel.top().pad(22, 30, 22, 30);

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, new Color(1f, 0.96f, 0.72f, 1f));
        Label.LabelStyle bodyStyle  = new Label.LabelStyle(bodyFont, Color.WHITE);
        Label.LabelStyle smallStyle = new Label.LabelStyle(smallFont, new Color(0.95f, 0.95f, 1f, 1f));
        Label.LabelStyle hintStyle  = new Label.LabelStyle(smallFont, new Color(1f, 0.96f, 0.72f, 1f));

        panel.add(new Label("PAUSE", titleStyle)).colspan(3).padBottom(14);
        panel.row();

        // Sliders
        Slider.SliderStyle ss = new Slider.SliderStyle();
        ss.background = drawable(roundRect(SLIDER_W, 12, 6, new Color(0.38f, 0.33f, 0.68f, 1f)));
        ss.knobBefore = drawable(roundRect(SLIDER_W, 12, 6, new Color(1f, 0.71f, 0.78f, 1f)));
        ss.knob       = drawable(circle(30, Color.WHITE));

        musicSlider = new Slider(0f, 1f, 0.05f, false, ss);
        musicValue  = new Label("", bodyStyle);
        musicSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                musicValue.setText(Math.round(musicSlider.getValue() * 100) + "%");
                if (!syncing) audio.setMusicVolume(musicSlider.getValue());
            }
        });

        sfxSlider = new Slider(0f, 1f, 0.05f, false, ss);
        sfxValue  = new Label("", bodyStyle);
        sfxSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                sfxValue.setText(Math.round(sfxSlider.getValue() * 100) + "%");
                if (!syncing) audio.setSfxVolume(sfxSlider.getValue());
            }
        });

        panel.add(new Label("Musique", bodyStyle)).left().width(170).padBottom(10);
        panel.add(musicSlider).width(SLIDER_W).height(34).padBottom(10);
        panel.add(musicValue).width(70).right().padBottom(10);
        panel.row();

        panel.add(new Label("Effets (SFX)", bodyStyle)).left().width(170).padBottom(10);
        panel.add(sfxSlider).width(SLIDER_W).height(34).padBottom(10);
        panel.add(sfxValue).width(70).right().padBottom(10);
        panel.row();

        // Plein ecran
        panel.add(new Label("Plein écran", bodyStyle)).left().width(170).padBottom(12);
        fullscreenBtn = button("", smallFont, 150, 40, new Color(0.72f, 0.89f, 1f, 1f), new Color(0.85f, 0.95f, 1f, 1f));
        fullscreenBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (Gdx.graphics.isFullscreen()) {
                    Gdx.graphics.setWindowedMode(1080, 720);
                    fullscreenBtn.setText("NON");
                } else {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    fullscreenBtn.setText("OUI");
                }
            }
        });
        panel.add(fullscreenBtn).width(150).height(40).left().colspan(2).padBottom(12);
        panel.row();

        // Rappel des controles
        panel.add(new Label("Contrôles", hintStyle)).colspan(3).padTop(6).padBottom(2);
        panel.row();
        panel.add(new Label("Déplacement : flèches ou A / D", smallStyle)).colspan(3);
        panel.row();
        panel.add(new Label("Saut : Espace, W ou flèche haut", smallStyle)).colspan(3);
        panel.row();
        panel.add(new Label("Pause : Échap ou P", smallStyle)).colspan(3).padBottom(6);
        panel.row();

        // Boutons
        TextButton resumeBtn = button("Reprendre", bodyFont, BTN_W, BTN_H,
            new Color(0.50f, 0.85f, 0.50f, 1f), new Color(0.65f, 0.93f, 0.65f, 1f));
        resumeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { callbacks.onResume(); }
        });
        panel.add(resumeBtn).width(BTN_W).height(BTN_H).colspan(3).padTop(10).padBottom(10);
        panel.row();

        TextButton quitBtn = button("Quitter la partie", bodyFont, BTN_W, BTN_H,
            new Color(1f, 0.55f, 0.65f, 1f), new Color(1f, 0.72f, 0.80f, 1f));
        quitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { showConfirm(true); }
        });
        panel.add(quitBtn).width(BTN_W).height(BTN_H).colspan(3);

        Table mainLayer = new Table();
        mainLayer.setFillParent(true);
        mainLayer.add(panel).size(PANEL_W, PANEL_H);
        stage.addActor(mainLayer);

        // ---------- CONFIRMATION "QUITTER ?" ----------
        confirmLayer = new Table();
        confirmLayer.setFillParent(true);
        confirmLayer.setBackground(drawable(solid(new Color(0.03f, 0.02f, 0.10f, 0.55f))));
        confirmLayer.setTouchable(Touchable.enabled); // bloque les clics sur le menu en dessous

        Table confirmPanel = new Table();
        confirmPanel.setBackground(drawable(roundRect(CONFIRM_W, CONFIRM_H, 36, new Color(1f, 0.80f, 0.86f, 0.98f))));
        confirmPanel.top().pad(24, 30, 24, 30);

        Label.LabelStyle confirmTitle = new Label.LabelStyle(titleFont, new Color(1f, 0.96f, 0.72f, 1f));
        confirmPanel.add(new Label("Quitter la partie ?", confirmTitle)).colspan(2).padBottom(16);
        confirmPanel.row();

        String msg = isHost
            ? "Vous êtes l'hôte : quitter arrêtera la partie pour TOUS les joueurs !"
            : "Vous retournerez au menu principal. Les autres joueurs continuent.";
        Label msgLabel = new Label(msg, bodyStyle);
        msgLabel.setWrap(true);
        msgLabel.setAlignment(Align.center);
        confirmPanel.add(msgLabel).colspan(2).width(CONFIRM_W - 80).padBottom(22);
        confirmPanel.row();

        TextButton yesBtn = button("Oui, quitter", bodyFont, 220, 52,
            new Color(1f, 0.45f, 0.58f, 1f), new Color(1f, 0.62f, 0.72f, 1f));
        yesBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { callbacks.onQuitConfirmed(); }
        });
        TextButton noBtn = button("Non", bodyFont, 220, 52,
            new Color(0.50f, 0.85f, 0.50f, 1f), new Color(0.65f, 0.93f, 0.65f, 1f));
        noBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { showConfirm(false); }
        });
        confirmPanel.add(yesBtn).width(220).height(52).padRight(16);
        confirmPanel.add(noBtn).width(220).height(52);

        confirmLayer.add(confirmPanel).size(CONFIRM_W, CONFIRM_H);
        confirmLayer.setVisible(false);
        stage.addActor(confirmLayer);

        syncFromSettings();
    }

    private void showConfirm(boolean visible) {
        confirmVisible = visible;
        confirmLayer.setVisible(visible);
    }

    /** Recale les curseurs / le bouton plein ecran sur l'etat reel (parametres partages). */
    private void syncFromSettings() {
        AudioManager audio = AudioManager.getInstance();
        syncing = true;
        musicSlider.setValue(audio.getMusicVolume());
        sfxSlider.setValue(audio.getSfxVolume());
        syncing = false;
        musicValue.setText(Math.round(audio.getMusicVolume() * 100) + "%");
        sfxValue.setText(Math.round(audio.getSfxVolume() * 100) + "%");
        fullscreenBtn.setText(Gdx.graphics.isFullscreen() ? "OUI" : "NON");
    }

    // ============================================================
    // API
    // ============================================================

    /** A appeler a l'ouverture du menu. */
    public void show() {
        showConfirm(false);
        syncFromSettings();
        Gdx.input.setInputProcessor(stage);
    }

    /** A appeler a la fermeture du menu. */
    public void hide() {
        showConfirm(false);
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
    }

    /**
     * Touche Echap / P : si la confirmation est ouverte on la ferme (et on reste en pause).
     * @return true si la touche a ete consommee
     */
    public boolean handleBack() {
        if (confirmVisible) {
            showConfirm(false);
            return true;
        }
        return false;
    }

    public void render(float delta) {
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        dim.setSize(stage.getWidth(), stage.getHeight());
    }

    public void dispose() {
        if (stage != null) { stage.dispose(); stage = null; }
        for (Texture t : textures) t.dispose();
        textures.clear();
    }
}
