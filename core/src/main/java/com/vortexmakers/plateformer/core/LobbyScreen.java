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
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.vortexmakers.plateformer.network.NetworkManager;
import com.vortexmakers.plateformer.network.listeners.NetworkListener;
import com.vortexmakers.plateformer.network.messages.*;

import java.util.HashMap;
import java.util.Map;

/**
 * LOBBYSCREEN - Avec sélection de personnage (style original amélioré)
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
    private String statusMessage = "Pret a jouer";

    // SÉLECTION DE PERSONNAGE
    private String selectedCharacter = "beige";
    private Map<String, TextButton> characterButtons;

    // Flag connexion
    private boolean connectionCheckStarted = false;
    private boolean gameScreenCreated = false; // ✅ NOUVEAU : Éviter les créations multiples

    public LobbyScreen(PlateformerGame game) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.networkManager.setNetworkListener(this);
        this.characterButtons = new HashMap<>();
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);
        batch = new SpriteBatch();
        font = new BitmapFont();
        skin = getSkin();

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        createUI();

        System.out.println("LobbyScreen démarré");
    }

    /**
     * CRÉATION DE L'INTERFACE - Style clair et lisible
     */
    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // ESPACE EN HAUT
        table.padTop(50);

        // ==========================================
        // SECTION 1 : CHOIX DU PERSONNAGE
        // ==========================================
        Label charLabel = new Label("Choix du personnage:", skin);
        charLabel.setFontScale(1.5f);
        table.add(charLabel).colspan(5).padBottom(15);
        table.row();

        // Boutons de personnage en ligne
        String[] characters = {"beige", "green", "pink", "purple", "yellow"};
        String[] labels = {"Beige", "Vert", "Rose", "Violet", "Jaune"};

        for (int i = 0; i < characters.length; i++) {
            String character = characters[i];
            TextButton button = new TextButton(labels[i], skin);

            // Mettre en surbrillance beige par défaut
            if (character.equals("beige")) {
                button.setColor(1, 1, 0.5f, 1); // Jaune clair
            }

            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectCharacter(character);
                }
            });

            characterButtons.put(character, button);
            table.add(button).width(120).height(60).pad(5);
        }
        table.row();

        // ESPACE
        table.add().height(40).colspan(5);
        table.row();

        // ==========================================
        // SECTION 2 : BOUTONS CONNEXION
        // ==========================================

        // Bouton héberger
        hostButton = new TextButton("Heberger une partie", skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hostGame();
            }
        });
        table.add(hostButton).width(250).height(60).padBottom(20).colspan(5);
        table.row();

        // Séparateur "OU"
        Label orLabel = new Label("--- OU ---", skin);
        orLabel.setFontScale(1.2f);
        table.add(orLabel).colspan(5).padBottom(15);
        table.row();

        // Champ IP
        ipField = new TextField("localhost", skin);
        ipField.setMessageText("Adresse IP du serveur");
        table.add(ipField).width(250).height(50).padBottom(20).colspan(5);
        table.row();

        // Bouton rejoindre
        joinButton = new TextButton("Rejoindre une partie", skin);
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinGame();
            }
        });
        table.add(joinButton).width(250).height(60).colspan(5);
    }

    /**
     * SÉLECTIONNER UN PERSONNAGE
     */
    private void selectCharacter(String character) {
        // Réinitialiser tous les boutons
        for (TextButton button : characterButtons.values()) {
            button.setColor(Color.WHITE);
        }

        // Mettre en surbrillance le sélectionné (jaune clair)
        TextButton selected = characterButtons.get(character);
        if (selected != null) {
            selected.setColor(1, 1, 0.5f, 1);
        }

        selectedCharacter = character;
        System.out.println("Personnage sélectionné : " + character);
    }

    private void hostGame() {
        statusMessage = "Demarrage du serveur...";
        hostButton.setDisabled(true);
        joinButton.setDisabled(true);

        // DÉFINIR LE PERSONNAGE AVANT DE SE CONNECTER
        networkManager.setLocalCharacter(selectedCharacter);

        networkManager.startHost();

        new Thread(() -> {
            try {
                Thread.sleep(500);
                Gdx.app.postRunnable(() -> {
                    statusMessage = "Connexion au serveur local...";
                    connectionCheckStarted = true;
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

        statusMessage = "Connexion a " + ip + "...";
        hostButton.setDisabled(true);
        joinButton.setDisabled(true);

        // ✅ DÉFINIR LE PERSONNAGE AVANT DE SE CONNECTER
        networkManager.setLocalCharacter(selectedCharacter);

        connectionCheckStarted = true;
        networkManager.connectToHost(ip);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Vérifier connexion SANS boucle infinie
        if (connectionCheckStarted && networkManager.isConnected() && !gameScreenCreated) {
            gameScreenCreated = true; // Empêcher les créations multiples

            System.out.println("[LOBBY] Connexion détectée, création GameScreen unique");

            statusMessage = "Connecte! Chargement du jeu...";

            // Créer le GameScreen
            game.setScreen(new GameScreen(game, networkManager, selectedCharacter));
            return;
        }

        stage.act(delta);
        stage.draw();

        // Statut
        batch.begin();
        font.draw(batch, statusMessage, 50, 50);
        batch.end();
    }

    // IMPLÉMENTATION NETWORKLISTENER
    @Override
    public void onConnectedToServer() {
        // Pas besoin de faire quoi que ce soit ici, géré dans render()
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
    public void onGameStateReceived(GameStateMessage message) {}

    @Override
    public void onDisconnectedFromServer() {
        Gdx.app.postRunnable(() -> {
            statusMessage = "Deconnecte du serveur";
            hostButton.setDisabled(false);
            joinButton.setDisabled(false);
            connectionCheckStarted = false;
            gameScreenCreated = false;
        });
    }

    @Override
    public void onConnectionFailed(String reason) {
        Gdx.app.postRunnable(() -> {
            statusMessage = "Erreur: " + reason;
            hostButton.setDisabled(false);
            joinButton.setDisabled(false);
            connectionCheckStarted = false;
            gameScreenCreated = false;
        });
    }

    @Override
    public void onPlatformStateReceived(PlatformStateMessage message) {}

    @Override
    public void onCollectibleStateReceived(CollectibleStateMessage message) {}

    @Override
    public void onGameTimerReceived(GameTimerMessage message) {}

    @Override
    public void onFinishFlagStateReceived(FinishFlagStateMessage message) {}

    @Override
    public void onSpikeStateReceived(SpikeStateMessage message) {}

    @Override
    public void onPlayerRespawned(PlayerRespawnMessage message) {}

    @Override
    public void onPlayerCharacterChanged(PlayerCharacterMessage message) {
        // Pas utilisé dans le lobby
    }

    /**
     * SKIN - Style original simple
     */
    private Skin getSkin() {
        Skin skin = new Skin();

        // Police
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // Texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        // Style TextButton
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        buttonStyle.down = skin.newDrawable("white", Color.GRAY);
        buttonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        buttonStyle.font = skin.getFont("default-font");
        skin.add("default", buttonStyle);

        // Style TextField
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default-font");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", Color.BLUE);
        skin.add("default", textFieldStyle);

        // Style Label
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

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
        batch.dispose();
        stage.dispose();
        font.dispose();
        if (skin != null) {
            skin.dispose();
        }
    }
}
