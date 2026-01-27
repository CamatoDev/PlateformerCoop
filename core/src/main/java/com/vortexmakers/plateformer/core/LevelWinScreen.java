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
 * LEVELWINSCREEN - Écran de victoire affiché quand tous les joueurs terminent le niveau
 *
 * Fonctionnalités :
 * - Affichage "VICTOIRE !"
 * - Classement des joueurs par score
 * - Scores finaux de chaque joueur
 * - Bouton "Niveau suivant" (grisé pour l'instant)
 * - Bouton "Retour au menu"
 */
public class LevelWinScreen implements Screen {
    private final PlateformerGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Stage stage;
    private BitmapFont titleFont;
    private BitmapFont scoreFont;
    private Skin skin;
    private GlyphLayout layout; // Pour mesurer la largeur du texte

    // Données de score
    private Map<Integer, PlayerScore> playerScores;
    private List<PlayerScore> rankedPlayers;

    // Référence réseau
    private NetworkManager networkManager;

    // Taille virtuelle (comme dans Constants)
    private static final int VIRTUAL_WIDTH = 1080;
    private static final int VIRTUAL_HEIGHT = 720;

    /**
     * CLASSE INTERNE POUR STOCKER LES DONNÉES D'UN JOUEUR
     */
    private static class PlayerScore implements Comparable<PlayerScore> {
        int playerId;
        String playerName;
        int score;

        PlayerScore(int playerId, String playerName, int score) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.score = score;
        }

        @Override
        public int compareTo(PlayerScore other) {
            return Integer.compare(other.score, this.score);
        }
    }

    public LevelWinScreen(PlateformerGame game, int localPlayerScore, Map<Integer, Integer> remotePlayerScores) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.layout = new GlyphLayout();

        // Initialisation des scores
        this.playerScores = new HashMap<>();

        int localPlayerId = networkManager.getLocalPlayerId();
        playerScores.put(localPlayerId, new PlayerScore(
            localPlayerId,
            networkManager.isHost() ? "You (Host)" : "You",
            localPlayerScore
        ));

        for (Map.Entry<Integer, Integer> entry : remotePlayerScores.entrySet()) {
            playerScores.put(entry.getKey(), new PlayerScore(
                entry.getKey(),
                "Player " + entry.getKey(),
                entry.getValue()
            ));
        }

        rankedPlayers = new ArrayList<>(playerScores.values());
        Collections.sort(rankedPlayers);

        System.out.println("LevelWinScreen créé avec " + rankedPlayers.size() + " joueurs");
    }

    @Override
    public void show() {
        // Caméra avec taille virtuelle fixe
        camera = new OrthographicCamera();
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch = new SpriteBatch();

        // Création des polices pixel-art style
        createPixelFonts();

        // Skin
        skin = createPixelSkin();

        // Stage avec viewport qui s'adapte
        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        createUI();

        System.out.println("LevelWinScreen affiché");
    }

    /**
     * CRÉATION DE POLICES PIXEL-ART
     */
    private void createPixelFonts() {
        // Police pour le titre
        titleFont = new BitmapFont();
        titleFont.getData().setScale(5.0f);
        titleFont.setColor(Color.GOLD);
        titleFont.setUseIntegerPositions(true); // Pixels nets
        titleFont.getData().markupEnabled = true;

        // Police pour les scores
        scoreFont = new BitmapFont();
        scoreFont.getData().setScale(2.5f);
        scoreFont.setUseIntegerPositions(true);
        scoreFont.getData().markupEnabled = true;
    }

    /**
     * CRÉATION DE L'INTERFACE UTILISATEUR
     */
    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Position des boutons en bas
        table.bottom().padBottom(50);

        // Bouton "Niveau suivant" (désactivé)
        TextButton nextLevelButton = new TextButton("Niveau suivant", skin);
        nextLevelButton.setDisabled(true);
        nextLevelButton.getLabel().setFontScale(1.2f);

        // Bouton "Retour au menu"
        TextButton menuButton = new TextButton("Retour au menu", skin);
        menuButton.getLabel().setFontScale(1.2f);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnToMenu();
            }
        });

        // Disposition horizontale des boutons
        table.add(menuButton).width(280).height(70).padRight(30);
        table.add(nextLevelButton).width(280).height(70);
    }

    /**
     * RETOUR AU MENU
     */
    private void returnToMenu() {
        System.out.println("🔙 Retour au menu...");

        // Déconnexion propre
        if (networkManager.isConnected()) {
            networkManager.disconnect();
        }

        // Petit délai
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
        // Fond vert victoire
        Gdx.gl.glClearColor(0.15f, 0.6f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Mise à jour
        stage.act(delta);

        // Rendu
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // TITRE "VICTOIRE !"
        titleFont.setColor(Color.GOLD);
        String title = "VICTOIRE !";
        layout.setText(titleFont, title);
        float titleX = (VIRTUAL_WIDTH - layout.width) / 2f;
        titleFont.draw(batch, title, titleX, VIRTUAL_HEIGHT - 60);

        // SOUS-TITRE
        scoreFont.setColor(Color.WHITE);
        String subtitle = "Tous les joueurs ont termine le niveau !";
        layout.setText(scoreFont, subtitle);
        float subtitleX = (VIRTUAL_WIDTH - layout.width) / 2f;
        scoreFont.draw(batch, subtitle, subtitleX, VIRTUAL_HEIGHT - 130);

        // CLASSEMENT
        renderRanking();

        batch.end();

        // Boutons
        stage.draw();
    }

    /**
     * AFFICHAGE DU CLASSEMENT
     */
    private void renderRanking() {
        float startY = VIRTUAL_HEIGHT - 220;
        float lineHeight = 50;
        float leftMargin = 150;

        // En-tête
        scoreFont.setColor(Color.YELLOW);
        scoreFont.draw(batch, "RANG", leftMargin, startY);
        scoreFont.draw(batch, "JOUEUR", leftMargin + 150, startY);
        scoreFont.draw(batch, "SCORE", VIRTUAL_WIDTH - 250, startY);

        startY -= 60;

        // Chaque joueur
        for (int i = 0; i < rankedPlayers.size(); i++) {
            PlayerScore player = rankedPlayers.get(i);
            float currentY = startY - (i * lineHeight);

            // Couleur selon rang
            Color rankColor;
            String medal = "";
            if (i == 0) {
                rankColor = Color.GOLD;
                medal = "[1st] "; // OU "★ " pour une étoile
            } else if (i == 1) {
                rankColor = new Color(0.75f, 0.75f, 0.75f, 1); // Argent
                medal = "[2nd] ";
            } else if (i == 2) {
                rankColor = new Color(0.8f, 0.5f, 0.2f, 1); // Bronze
                medal = "[3rd] ";
            } else {
                rankColor = Color.WHITE;
                medal = "[" + (i + 1) + "] ";
            }

            scoreFont.setColor(rankColor);

            // Rang
            scoreFont.draw(batch, medal, leftMargin, currentY);

            // Nom
            scoreFont.draw(batch, player.playerName, leftMargin + 150, currentY);

            // Score
            scoreFont.draw(batch, player.score + " coins", VIRTUAL_WIDTH - 250, currentY);
        }
    }

    /**
     * SKIN PIXEL-ART
     */
    private Skin createPixelSkin() {
        Skin skin = new Skin();

        // Police pour boutons
        BitmapFont buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.8f);
        buttonFont.setUseIntegerPositions(true);
        skin.add("default-font", buttonFont);

        // Texture
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        // Style bouton
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", new Color(0.2f, 0.5f, 0.8f, 1));
        buttonStyle.down = skin.newDrawable("white", new Color(0.1f, 0.4f, 0.7f, 1));
        buttonStyle.over = skin.newDrawable("white", new Color(0.3f, 0.6f, 0.9f, 1));
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
        if (scoreFont != null) scoreFont.dispose();
        if (skin != null) skin.dispose();
        System.out.println("LevelWinScreen nettoyé");
    }
}
