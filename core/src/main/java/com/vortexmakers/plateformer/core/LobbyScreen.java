package com.vortexmakers.plateformer.core;

// IMPORTATIONS LIBGDX
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
// IMPORTATIONS GRAPHIQUES POUR LE SKIN
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;

// IMPORTATIONS PROJET
import com.vortexmakers.plateformer.network.NetworkManager;
import com.vortexmakers.plateformer.network.listeners.NetworkListener;
import com.vortexmakers.plateformer.network.messages.*;

/**
 * LOBBYSCREEN - Écran de connexion multijoueur simple
 *
 * Fonctionnalités basiques :
 * - Héberger une partie
 * - Rejoindre avec une IP
 * - Message de statut
 */

public class LobbyScreen implements Screen, NetworkListener {
    private final PlateformerGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Stage stage;
    private BitmapFont font;
    private Skin skin;

    // RÉSEAU
    private NetworkManager networkManager;

    // INTERFACE
    private TextField ipField;
    private TextButton hostButton;
    private TextButton joinButton;
    private String statusMessage = "Prêt à jouer";

    public LobbyScreen(PlateformerGame game) {
        this.game = game;
        // ✅ UTILISATION DU SINGLETON
        this.networkManager = NetworkManager.getInstance();
        this.networkManager.setNetworkListener(this);
    }

    @Override
    public void show() {
        // Initialisation graphique
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);
        batch = new SpriteBatch();
        font = new BitmapFont();
        skin = getSkin();

        // Initialisation stage
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Création UI
        createUI();

        System.out.println("LobbyScreen démarré");
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Bouton héberger
        hostButton = new TextButton("Héberger une partie", skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hostGame();
            }
        });

        // Champ IP
        ipField = new TextField("localhost", skin);
        ipField.setMessageText("Adresse IP du serveur");

        // Bouton rejoindre
        joinButton = new TextButton("Rejoindre une partie", skin);
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinGame();
            }
        });

        // Disposition
        table.add(hostButton).width(200).height(50).padBottom(20);
        table.row();
        table.add(ipField).width(200).height(50).padBottom(20);
        table.row();
        table.add(joinButton).width(200).height(50);
    }

    private void hostGame() {
        statusMessage = "Démarrage du serveur...";
        hostButton.setDisabled(true);
        joinButton.setDisabled(true);

        networkManager.startHost();

        // Attendre que le serveur démarre, puis se connecter en client
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Gdx.app.postRunnable(() -> {
                    statusMessage = "Connexion au serveur local...";
                    networkManager.connectToHost("localhost");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void joinGame() {
        String ip = ipField.getText();
        if (ip.isEmpty()) {
            statusMessage = "Veuillez entrer une adresse IP";
            return;
        }

        statusMessage = "Connexion à " + ip + "...";
        hostButton.setDisabled(true);
        joinButton.setDisabled(true);

        networkManager.connectToHost(ip);
    }

    private Skin getSkin() {
        // SOLUTION 1 : Skin par défaut de LibGDX (si disponible)
        try {
            // Essaie de charger le skin par défaut de LibGDX
            return new Skin(Gdx.files.internal("uiskin.json"));
        } catch (Exception e) {
            // Si le skin par défaut n'existe pas, on crée un skin basique
            return createBasicSkin();
        }
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();

        // Police basique
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // Texture blanche pour les fonds
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        // Style pour TextButton
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        buttonStyle.down = skin.newDrawable("white", Color.GRAY);
        buttonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        buttonStyle.font = skin.getFont("default-font");
        skin.add("default", buttonStyle);

        // Style pour TextField
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default-font");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", Color.BLUE);
        skin.add("default", textFieldStyle);

        return skin;
    }

    @Override
    public void render(float delta) {
        // Nettoyage écran
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Mise à jour stage
        stage.act(delta);
        stage.draw();

        // Affichage statut
        batch.begin();
        font.draw(batch, statusMessage, 50, 50);
        batch.end();
    }

    // IMPLÉMENTATION NETWORKLISTENER
    @Override
    public void onConnectedToServer() {
        Gdx.app.postRunnable(() -> {
            statusMessage = "Connecté! Chargement du jeu...";
            game.setScreen(new GameScreen(game, networkManager));
        });
    }

    @Override
    public void onPlayerJoined(PlayerJoinMessage message) {
        System.out.println("Joueur rejoint: " + message.playerName);
    }

    @Override
    public void onPlayerLeft(PlayerLeaveMessage message) {
        System.out.println("Joueur parti: " + message.playerId);
    }

    @Override
    public void onGameStateReceived(GameStateMessage message) {
        // Pas utilisé dans le lobby
    }

    @Override
    public void onDisconnectedFromServer() {
        Gdx.app.postRunnable(() -> {
            statusMessage = "Déconnecté du serveur";
            hostButton.setDisabled(false);
            joinButton.setDisabled(false);
        });
    }

    @Override
    public void onConnectionFailed(String reason) {
        Gdx.app.postRunnable(() -> {
            statusMessage = "Erreur: " + reason;
            hostButton.setDisabled(false);
            joinButton.setDisabled(false);
        });
    }

    @Override
    public void onPlatformStateReceived(PlatformStateMessage message) {
        // Pas utilisé dans le lobby
        System.out.println("🏗️ État plateformes reçu (lobby)");
    }

    // ✅ AJOUTER CETTE MÉTHODE MANQUANTE :
    @Override
    public void onCollectibleStateReceived(CollectibleStateMessage message) {
        // Pas utilisé dans le lobby - laisser vide
        System.out.println("📦 État collectibles reçu (lobby) - " + message.collectibles.size() + " collectibles");
    }

    // AUTRES MÉTHODES SCREEN ===========================

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
        font.dispose();
        if (skin != null) {
            skin.dispose();
        }
    }
}
