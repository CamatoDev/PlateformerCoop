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

    // Données de score
    private Map<Integer, PlayerScore> playerScores;
    private List<PlayerScore> rankedPlayers;

    // Référence réseau
    private NetworkManager networkManager;

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
            // Tri décroissant (score le plus élevé en premier)
            return Integer.compare(other.score, this.score);
        }
    }

    /**
     * CONSTRUCTEUR
     *
     * @param game Référence au jeu principal
     * @param localPlayerScore Score du joueur local
     * @param remotePlayerScores Map des scores des joueurs distants
     */
    public LevelWinScreen(PlateformerGame game, int localPlayerScore, Map<Integer, Integer> remotePlayerScores) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();

        // Initialisation des scores
        this.playerScores = new HashMap<>();

        // Ajouter le joueur local
        int localPlayerId = networkManager.getLocalPlayerId();
        playerScores.put(localPlayerId, new PlayerScore(
            localPlayerId,
            networkManager.isHost() ? "You (Host)" : "You",
            localPlayerScore
        ));

        // Ajouter les joueurs distants
        for (Map.Entry<Integer, Integer> entry : remotePlayerScores.entrySet()) {
            playerScores.put(entry.getKey(), new PlayerScore(
                entry.getKey(),
                "Player " + entry.getKey(),
                entry.getValue()
            ));
        }

        // Créer le classement
        rankedPlayers = new ArrayList<>(playerScores.values());
        Collections.sort(rankedPlayers);

        System.out.println("🏆 LevelWinScreen créé avec " + rankedPlayers.size() + " joueurs");
    }

    @Override
    public void show() {
        // Initialisation graphique
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
        batch = new SpriteBatch();

        // Polices
        titleFont = new BitmapFont();
        titleFont.getData().setScale(4.0f); // Grand titre
        titleFont.setColor(Color.GOLD);

        scoreFont = new BitmapFont();
        scoreFont.getData().setScale(2.0f);

        // Création du skin et de l'UI
        skin = createSkin();

        // Stage pour les boutons
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        createUI();

        System.out.println("LevelWinScreen affiché");
    }

    /**
     * CRÉATION DE L'INTERFACE UTILISATEUR
     */
    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Espacement pour laisser de la place au titre et aux scores
        table.padTop(400);

        // Bouton "Niveau suivant" (désactivé)
        TextButton nextLevelButton = new TextButton("Niveau suivant", skin);
        nextLevelButton.setDisabled(true); // Grisé
        nextLevelButton.setColor(Color.GRAY);

        // Bouton "Retour au menu"
        TextButton menuButton = new TextButton("Retour au menu", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnToMenu();
            }
        });

        // Disposition des boutons
        table.add(nextLevelButton).width(250).height(60).padBottom(20);
        table.row();
        table.add(menuButton).width(250).height(60);
    }

    /**
     * RETOUR AU MENU (LOBBYSCREEN)
     */
    private void returnToMenu() {
        System.out.println("🔙 Retour au menu...");

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
        // Fond vert victoire
        Gdx.gl.glClearColor(0.1f, 0.5f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Mise à jour du stage
        stage.act(delta);

        // Rendu
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // TITRE "VICTOIRE !"
        String title = "VICTOIRE !";
        float titleWidth = titleFont.getData().scaleX * title.length() * 30; // Estimation
        float titleX = (Constants.SCREEN_WIDTH - titleWidth) / 2f;
        titleFont.draw(batch, title, titleX, Constants.SCREEN_HEIGHT - 50);

        // SOUS-TITRE
        scoreFont.setColor(Color.WHITE);
        String subtitle = "Tous les joueurs ont termine le niveau !";
        float subtitleWidth = scoreFont.getData().scaleX * subtitle.length() * 15;
        float subtitleX = (Constants.SCREEN_WIDTH - subtitleWidth) / 2f;
        scoreFont.draw(batch, subtitle, subtitleX, Constants.SCREEN_HEIGHT - 120);

        // CLASSEMENT DES JOUEURS
        renderRanking();

        batch.end();

        // Dessiner les boutons
        stage.draw();
    }

    /**
     * AFFICHAGE DU CLASSEMENT
     */
    private void renderRanking() {
        float startY = Constants.SCREEN_HEIGHT - 200;
        float lineHeight = 40;

        // En-tête du classement
        scoreFont.setColor(Color.YELLOW);
        scoreFont.draw(batch, "CLASSEMENT", 100, startY);
        scoreFont.draw(batch, "SCORE", Constants.SCREEN_WIDTH - 250, startY);

        startY -= 50;

        // Afficher chaque joueur
        for (int i = 0; i < rankedPlayers.size(); i++) {
            PlayerScore player = rankedPlayers.get(i);

            // Couleur selon le rang
            if (i == 0) {
                scoreFont.setColor(Color.GOLD); // 1er = Or
            } else if (i == 1) {
                scoreFont.setColor(Color.LIGHT_GRAY); // 2e = Argent
            } else if (i == 2) {
                scoreFont.setColor(new Color(0.8f, 0.5f, 0.2f, 1)); // 3e = Bronze
            } else {
                scoreFont.setColor(Color.WHITE);
            }

            // Rang et nom
            String rankText = (i + 1) + ". " + player.playerName;
            scoreFont.draw(batch, rankText, 100, startY - (i * lineHeight));

            // Score avec médaille pour le 1er
            String scoreText = player.score + " coins";
            if (i == 0) {
                scoreText = "🏆 " + scoreText;
            }
            scoreFont.draw(batch, scoreText, Constants.SCREEN_WIDTH - 250, startY - (i * lineHeight));
        }
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
        buttonStyle.up = skin.newDrawable("white", new Color(0.2f, 0.4f, 0.6f, 1));
        buttonStyle.down = skin.newDrawable("white", new Color(0.1f, 0.3f, 0.5f, 1));
        buttonStyle.over = skin.newDrawable("white", new Color(0.3f, 0.5f, 0.7f, 1));
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
        if (scoreFont != null) {
            scoreFont.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        System.out.println("LevelWinScreen nettoyé");
    }
}
