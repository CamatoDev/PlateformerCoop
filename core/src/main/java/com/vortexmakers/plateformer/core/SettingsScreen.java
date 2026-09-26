package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class SettingsScreen implements Screen {
    private final PlateformerGame game;
    private SpriteBatch batch;
    private Stage stage;
    private BitmapFont titleFont, labelFont, smallFont;
    private Texture background, panelTex, btnReturnTex;
    private boolean isFullscreen = false;

    // Valeurs (pour usage futur quand le son sera intégré)
    public static float musicVolume = 0.8f;
    public static float sfxVolume = 1.0f;

    private static final int VW = 1080;
    private static final int VH = 720;
    private static final int PANEL_W = 620;
    private static final int PANEL_H = 500;

    public SettingsScreen(PlateformerGame game) {
        this.game = game;
    }

    private Texture createRoundedRect(int w, int h, Color color) {
        Pixmap px = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        px.setColor(color);
        int r = Math.min(h / 2, 20);
        px.fillRectangle(r, 0, w - 2*r, h);
        px.fillRectangle(0, r, w, h - 2*r);
        px.fillCircle(r, r, r);
        px.fillCircle(w - r, r, r);
        px.fillCircle(r, h - r, r);
        px.fillCircle(w - r, h - r, r);
        Texture t = new Texture(px);
        px.dispose();
        return t;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new FitViewport(VW, VH));

        com.vortexmakers.plateformer.utils.FontManager.getInstance().load();
        titleFont = com.vortexmakers.plateformer.utils.FontManager.getInstance().getSubtitle();
        labelFont = com.vortexmakers.plateformer.utils.FontManager.getInstance().getBody();
        smallFont = com.vortexmakers.plateformer.utils.FontManager.getInstance().getSmall();

        final com.vortexmakers.plateformer.utils.AudioManager audio =
            com.vortexmakers.plateformer.utils.AudioManager.getInstance();

        background  = new Texture(Gdx.files.internal("Backgrounds/background_color_hills.png"));
        panelTex    = createRoundedRect(PANEL_W, PANEL_H, new Color(0.72f,0.89f,1f,0.93f));

        // Textures bouton avec état hover
        btnReturnTex = createRoundedRect(250, 65, new Color(1f,0.71f,0.78f,1f));
        Texture btnHovTex = createRoundedRect(250, 65, new Color(1f,0.87f,0.91f,1f));
        Texture btnFsTex  = createRoundedRect(220, 55, new Color(0.74f,0.85f,1f,1f));
        Texture btnFsHov  = createRoundedRect(220, 55, new Color(0.88f,0.94f,1f,1f));

        Skin skin = new Skin();
        skin.add("default-font", labelFont);
        Pixmap wPx = new Pixmap(1,1,Pixmap.Format.RGBA8888); wPx.setColor(Color.WHITE); wPx.fill();
        skin.add("white", new Texture(wPx)); wPx.dispose();

        Label.LabelStyle ls = new Label.LabelStyle(labelFont, Color.WHITE);
        skin.add("default", ls);

        // Slider style
        Slider.SliderStyle ss = new Slider.SliderStyle();
        Pixmap trackPx = new Pixmap(200, 8, Pixmap.Format.RGBA8888);
        trackPx.setColor(new Color(0.5f,0.7f,0.9f,1f)); trackPx.fill();
        ss.background = new TextureRegionDrawable(new TextureRegion(new Texture(trackPx))); trackPx.dispose();
        Pixmap knobPx = new Pixmap(20,20,Pixmap.Format.RGBA8888);
        knobPx.setColor(Color.WHITE); knobPx.fillCircle(10,10,10);
        ss.knob = new TextureRegionDrawable(new TextureRegion(new Texture(knobPx))); knobPx.dispose();

        // Style bouton Retour (avec hover)
        TextButton.TextButtonStyle returnStyle = new TextButton.TextButtonStyle();
        returnStyle.font = labelFont; returnStyle.fontColor = Color.WHITE;
        returnStyle.up   = new TextureRegionDrawable(new TextureRegion(btnReturnTex));
        returnStyle.over = new TextureRegionDrawable(new TextureRegion(btnHovTex));
        returnStyle.down = new TextureRegionDrawable(new TextureRegion(btnReturnTex));

        // Style bouton Fullscreen (avec hover)
        TextButton.TextButtonStyle fsStyle = new TextButton.TextButtonStyle();
        fsStyle.font = smallFont; fsStyle.fontColor = new Color(0.2f,0.3f,0.6f,1f);
        fsStyle.up   = new TextureRegionDrawable(new TextureRegion(btnFsTex));
        fsStyle.over = new TextureRegionDrawable(new TextureRegion(btnFsHov));
        fsStyle.down = new TextureRegionDrawable(new TextureRegion(btnFsTex));

        skin.add("default", returnStyle);

        // Sliders connectés à AudioManager
        Slider musicSlider = new Slider(0, 1, 0.05f, false, ss);
        musicSlider.setValue(audio.getMusicVolume());
        musicSlider.addListener(e -> {
            musicVolume = musicSlider.getValue();
            audio.setMusicVolume(musicVolume);
            return false;
        });

        Slider sfxSlider = new Slider(0, 1, 0.05f, false, ss);
        sfxSlider.setValue(audio.getSfxVolume());
        sfxSlider.addListener(e -> {
            sfxVolume = sfxSlider.getValue();
            audio.setSfxVolume(sfxVolume);
            return false;
        });

        // Bouton Plein écran
        TextButton fullscreenBtn = new TextButton(isFullscreen ? "Desactiver" : "Activer", fsStyle);
        fullscreenBtn.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                audio.playClick();
                isFullscreen = !isFullscreen;
                if (isFullscreen) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                    fullscreenBtn.setText("Desactiver");
                } else {
                    Gdx.graphics.setWindowedMode(1080, 720);
                    fullscreenBtn.setText("Activer");
                }
            }
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                audio.playRollover();
            }
        });

        // Bouton Retour
        TextButton returnBtn = new TextButton("< Retour", returnStyle);
        returnBtn.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                audio.playClick();
                game.setScreen(new TitleScreen(game));
            }
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                audio.playRollover();
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.padTop(20);

        table.add(new Label("Volume Musique", skin)).left().padRight(20);
        table.add(musicSlider).width(300).padBottom(30); table.row();

        table.add(new Label("Volume SFX", skin)).left().padRight(20);
        table.add(sfxSlider).width(300).padBottom(30); table.row();

        table.add(new Label("Plein ecran", skin)).left().padRight(20);
        table.add(fullscreenBtn).width(220).height(55).padBottom(30); table.row();

        table.add(new Label("Controles :", skin)).left().colspan(2).padBottom(8); table.row();
        table.add(new Label("<- -> : Deplacement | Espace : Saut", skin)).colspan(2).padBottom(40); table.row();

        table.add(returnBtn).width(250).height(65).colspan(2);

        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.5f,0.7f,0.9f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background, 0, 0, VW, VH);

        // Panneau central ombre
        batch.setColor(0,0,0,0.3f);
        batch.draw(panelTex, (VW-PANEL_W)/2f+5, (VH-PANEL_H)/2f-5, PANEL_W, PANEL_H);
        batch.setColor(1,1,1,1);
        batch.draw(panelTex, (VW-PANEL_W)/2f, (VH-PANEL_H)/2f, PANEL_W, PANEL_H);

        // Titre du panneau
        titleFont.setColor(0,0,0,0.5f);
        titleFont.draw(batch, "Parametres", (VW-PANEL_W)/2f+22, VH/2f+PANEL_H/2f-25+3);
        titleFont.setColor(0.2f,0.3f,0.6f,1f);
        titleFont.draw(batch, "Parametres", (VW-PANEL_W)/2f+20, VH/2f+PANEL_H/2f-25);

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        if(batch != null) batch.dispose();
        if(stage != null) stage.dispose();
        if(titleFont != null) titleFont.dispose(); 
        if(labelFont != null) labelFont.dispose(); 
        if(smallFont != null) smallFont.dispose();
        if(background != null) background.dispose();
        if(panelTex != null) panelTex.dispose();
        if(btnReturnTex != null) btnReturnTex.dispose();
    }
}
