package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.vortexmakers.plateformer.network.NetworkManager;
import com.vortexmakers.plateformer.utils.Constants;

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

    // Données de la partie
    private int localPlayerScore;
    private Map<Integer, Integer> remotePlayerScores;
    private float timeElapsed;

    // Référence réseau
    private NetworkManager networkManager;

    /**
     * CONSTRUCTEUR
     *
     * @param game Référence au jeu principal
     * @param localPlayerScore Score du joueur local
     * @param remotePlayerScores Map des scores des joueurs distants
     * @param timeElapsed Temps écoulé avant le game over
     */
    public GameOverScreen(PlateformerGame game, int localPlayerScore,
                          Map<Integer, Integer> remotePlayerScores, float timeElapsed) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.localPlayerScore = localPlayerScore;
        this.remotePlayerScores = new HashMap<>(remotePlayerScores);
        this.timeElapsed = timeElapsed;

        System.out.println("💀 GameOverScreen créé - Temps: " + formatTime((int)timeElapsed));
    }

    @Override
    public void show() {
        // Initialisation graphique
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
        batch = new SpriteBatch();

        // Polices
        titleFont = new BitmapFont();
        titleFont.getData().setScale(4.0f);
        titleFont.setColor(Color.RED);

        textFont = new BitmapFont();
        textFont.getData().setScale(2.0f);
        textFont.setColor(Color.WHITE);

        // Création du skin et de l'UI
        skin = createSkin();

        // Stage pour les boutons
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        createUI();

        System.out.println("GameOverScreen affiché");
    }

    /**
     * CRÉATION DE L'INTERFACE UTILISATEUR
     */
    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Espacement pour laisser de la place au titre et aux stats
        table.padTop(400);

        // Bouton "Réessayer" (désactivé pour l'instant)
        TextButton retryButton = new TextButton("Reessayer", skin);
        retryButton.setDisabled(true);
        retryButton.setColor(Color.GRAY);

        // Bouton "Retour au menu"
        TextButton menuButton = new TextButton("Retour au menu", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnToMenu();
            }
        });

        // Disposition des boutons
        table.add(retryButton).width(250).height(60).padBottom(20);
        table.row();
        table.add(menuButton).width(250).height(60);
    }

    /**
     * RETOUR AU MENU (LOBBYSCREEN)
     */
    private void returnToMenu() {
        System.out.println("Retour au menu...");

        // Déconnexion propre
        if (networkManager.isConnected()) {
            networkManager.disconnect();
        }

        // Petit délai pour laisser la déconnexion se faire
        new Thread(() -> {
            try {
                Thread.sleep(200);
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
        // Fond rouge sombre (échec)
        Gdx.gl.glClearColor(0.3f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Mise à jour du stage
        stage.act(delta);

        // Rendu
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // TITRE "TEMPS ÉCOULÉ !"
        String title = "TEMPS ECOULE !";
        float titleWidth = titleFont.getData().scaleX * title.length() * 30;
        float titleX = (Constants.SCREEN_WIDTH - titleWidth) / 2f;
        titleFont.draw(batch, title, titleX, Constants.SCREEN_HEIGHT - 50);

        // SOUS-TITRE
        String subtitle = "Vous n'avez pas termine a temps...";
        float subtitleWidth = textFont.getData().scaleX * subtitle.length() * 15;
        float subtitleX = (Constants.SCREEN_WIDTH - subtitleWidth) / 2f;
        textFont.draw(batch, subtitle, subtitleX, Constants.SCREEN_HEIGHT - 120);

        // STATISTIQUES DE LA PARTIE
        renderStats();

        batch.end();

        // Dessiner les boutons
        stage.draw();
    }

    /**
     * AFFICHAGE DES STATISTIQUES
     */
    private void renderStats() {
        float startY = Constants.SCREEN_HEIGHT - 200;
        float lineHeight = 40;

        // Temps écoulé
        textFont.setColor(Color.YELLOW);
        String timeText = "Temps: " + formatTime((int)timeElapsed);
        textFont.draw(batch, timeText, 100, startY);

        startY -= 60;

        // Scores des joueurs
        textFont.setColor(Color.WHITE);
        textFont.draw(batch, "SCORES FINAUX", 100, startY);

        startY -= 50;

        // Score du joueur local
        int localPlayerId = networkManager.getLocalPlayerId();
        String localName = networkManager.isHost() ? "You (Host)" : "You";
        textFont.setColor(Color.CYAN);
        textFont.draw(batch, localName + ": " + localPlayerScore + " coins", 120, startY);

        startY -= lineHeight;

        // Scores des joueurs distants
        textFont.setColor(Color.LIGHT_GRAY);
        for (Map.Entry<Integer, Integer> entry : remotePlayerScores.entrySet()) {
            String playerText = "Player " + entry.getKey() + ": " + entry.getValue() + " coins";
            textFont.draw(batch, playerText, 120, startY);
            startY -= lineHeight;
        }

        // Message d'encouragement
        startY -= 40;
        textFont.setColor(Color.ORANGE);
        String encouragement = "Essayez encore pour battre votre score !";
        float encWidth = textFont.getData().scaleX * encouragement.length() * 15;
        float encX = (Constants.SCREEN_WIDTH - encWidth) / 2f;
        textFont.draw(batch, encouragement, encX, startY);
    }

    /**
     * FORMATER LE TEMPS (MM:SS)
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * CRÉATION D'UN SKIN BASIQUE
     */
    private Skin createSkin() {
        Skin skin = new Skin();

        // Police
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);

        // Texture blanche pour les fonds
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        // Style pour TextButton
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", new Color(0.6f, 0.2f, 0.2f, 1));
        buttonStyle.down = skin.newDrawable("white", new Color(0.5f, 0.1f, 0.1f, 1));
        buttonStyle.over = skin.newDrawable("white", new Color(0.7f, 0.3f, 0.3f, 1));
        buttonStyle.disabled = skin.newDrawable("white", Color.DARK_GRAY);
        buttonStyle.font = skin.getFont("default-font");
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.disabledFontColor = Color.GRAY;
        skin.add("default", buttonStyle);

        return skin;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (stage != null) {
            stage.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (textFont != null) {
            textFont.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        System.out.println("GameOverScreen nettoyé");
    }
}
