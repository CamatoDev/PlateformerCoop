package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.vortexmakers.plateformer.network.NetworkManager;

import java.util.*;

/**
 * GAMEOVERSCREEN - Écran affiché quand le temps est écoulé
 *
 * Fonctionnalités :
 * - Affichage "TEMPS ÉCOULÉ !"
 * - Statistiques de la partie (scores, temps)
 * - Bouton "Réessayer" (grisé pour l'instant)
 * - Bouton "Retour au menu"
 */
public class GameOverScreen implements Screen {
    private final PlateformerGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Stage stage;
    private BitmapFont titleFont;
    private BitmapFont textFont;
    private Skin skin;
    private GlyphLayout layout;

    // Données
    private int localPlayerScore;
    private Map<Integer, Integer> remotePlayerScores;
    private float timeElapsed;
    private NetworkManager networkManager;

    // Taille virtuelle
    private static final int VIRTUAL_WIDTH = 1080;
    private static final int VIRTUAL_HEIGHT = 720;

    public GameOverScreen(PlateformerGame game, int localPlayerScore,
                          Map<Integer, Integer> remotePlayerScores, float timeElapsed) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.localPlayerScore = localPlayerScore;
        this.remotePlayerScores = new HashMap<>(remotePlayerScores);
        this.timeElapsed = timeElapsed;
        this.layout = new GlyphLayout();

        System.out.println("GameOverScreen créé - Temps: " + formatTime((int)timeElapsed));
    }

    @Override
    public void show() {
        // Caméra
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch = new SpriteBatch();

        // Polices
        createPixelFonts();

        // Skin
        skin = createPixelSkin();

        // Stage
        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        createUI();

        System.out.println("GameOverScreen affiché");
    }

    /**
     * CRÉATION POLICES
     */
    private void createPixelFonts() {
        titleFont = new BitmapFont();
        titleFont.getData().setScale(5.0f);
        titleFont.setColor(Color.RED);
        titleFont.setUseIntegerPositions(true);

        textFont = new BitmapFont();
        textFont.getData().setScale(2.5f);
        textFont.setUseIntegerPositions(true);
    }

    /**
     * CRÉATION UI
     */
    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        table.bottom().padBottom(50);

        // Bouton "Réessayer" (désactivé)
        TextButton retryButton = new TextButton("Reessayer", skin);
        retryButton.setDisabled(true);
        retryButton.getLabel().setFontScale(1.2f);

        // Bouton "Retour au menu"
        TextButton menuButton = new TextButton("Retour au menu", skin);
        menuButton.getLabel().setFontScale(1.2f);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnToMenu();
            }
        });

        // Disposition
        table.add(menuButton).width(280).height(70).padRight(30);
        table.add(retryButton).width(280).height(70);
    }

    /**
     * RETOUR AU MENU
     */
    private void returnToMenu() {
        System.out.println("🔙 Retour au menu...");

        if (networkManager.isConnected()) {
            networkManager.disconnect();
        }

        new Thread(() -> {
            try {
                Thread.sleep(300);
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new LobbyScreen(game));
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void render(float delta) {
        // Fond rouge sombre
        Gdx.gl.glClearColor(0.35f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // TITRE
        titleFont.setColor(Color.RED);
        String title = "TEMPS ECOULE !";
        layout.setText(titleFont, title);
        float titleX = (VIRTUAL_WIDTH - layout.width) / 2f;
        titleFont.draw(batch, title, titleX, VIRTUAL_HEIGHT - 60);

        // SOUS-TITRE
        textFont.setColor(Color.WHITE);
        String subtitle = "Vous n'avez pas termine a temps...";
        layout.setText(textFont, subtitle);
        float subtitleX = (VIRTUAL_WIDTH - layout.width) / 2f;
        textFont.draw(batch, subtitle, subtitleX, VIRTUAL_HEIGHT - 130);

        // STATS
        renderStats();

        batch.end();

        stage.draw();
    }

    /**
     * AFFICHAGE STATS
     */
    private void renderStats() {
        float startY = VIRTUAL_HEIGHT - 220;
        float lineHeight = 50;
        float leftMargin = 150;

        // Temps
        textFont.setColor(Color.ORANGE);
        String timeText = "Temps ecoule: " + formatTime((int)timeElapsed);
        textFont.draw(batch, timeText, leftMargin, startY);

        startY -= 80;

        // Titre scores
        textFont.setColor(Color.YELLOW);
        textFont.draw(batch, "SCORES FINAUX", leftMargin, startY);

        startY -= 60;

        // Score local
        int localPlayerId = networkManager.getLocalPlayerId();
        String localName = networkManager.isHost() ? "You (Host)" : "You";
        textFont.setColor(Color.CYAN);
        textFont.draw(batch, localName + ": " + localPlayerScore + " coins", leftMargin + 30, startY);

        startY -= lineHeight;

        // Scores distants
        textFont.setColor(Color.LIGHT_GRAY);
        for (Map.Entry<Integer, Integer> entry : remotePlayerScores.entrySet()) {
            String playerText = "Player " + entry.getKey() + ": " + entry.getValue() + " coins";
            textFont.draw(batch, playerText, leftMargin + 30, startY);
            startY -= lineHeight;
        }

        // Message encouragement
        startY -= 30;
        textFont.setColor(Color.ORANGE);
        String encouragement = "Reessayez pour ameliorer votre score !";
        layout.setText(textFont, encouragement);
        float encX = (VIRTUAL_WIDTH - layout.width) / 2f;
        textFont.draw(batch, encouragement, encX, startY);
    }

    /**
     * FORMATAGE TEMPS
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * SKIN
     */
    private Skin createPixelSkin() {
        Skin skin = new Skin();

        BitmapFont buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.8f);
        buttonFont.setUseIntegerPositions(true);
        skin.add("default-font", buttonFont);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", new Color(0.7f, 0.2f, 0.2f, 1));
        buttonStyle.down = skin.newDrawable("white", new Color(0.6f, 0.1f, 0.1f, 1));
        buttonStyle.over = skin.newDrawable("white", new Color(0.8f, 0.3f, 0.3f, 1));
        buttonStyle.disabled = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 1));
        buttonStyle.font = skin.getFont("default-font");
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.disabledFontColor = new Color(0.6f, 0.6f, 0.6f, 1);
        skin.add("default", buttonStyle);

        return skin;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        if (titleFont != null) titleFont.dispose();
        if (textFont != null) textFont.dispose();
        if (skin != null) skin.dispose();
        System.out.println("GameOverScreen nettoyé");
    }
}
