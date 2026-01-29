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
 * LOBBYSCREEN - Avec sélection de personnage et liste des joueurs
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

    // ✅ NOUVEAU : LISTE DES JOUEURS
    private Table playerListTable;
    private Map<Integer, LobbyStateMessage.LobbyPlayerData> lobbyPlayers;

    // Flag connexion
    private boolean connectionCheckStarted = false;
    private boolean gameScreenCreated = false;

    public LobbyScreen(PlateformerGame game) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.networkManager.setNetworkListener(this);
        this.characterButtons = new HashMap<>();
        this.lobbyPlayers = new HashMap<>();
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
     * CRÉATION DE L'INTERFACE - Avec liste des joueurs
     */
    private void createUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        mainTable.padTop(20);

        // ==========================================
        // SECTION 1 : LISTE DES JOUEURS
        // ==========================================
        Label playersLabel = new Label("Joueurs connectes:", skin);
        playersLabel.setFontScale(1.5f);
        mainTable.add(playersLabel).colspan(5).padBottom(10);
        mainTable.row();

        // ✅ NOUVEAU : Table pour la liste des joueurs
        playerListTable = new Table();
        playerListTable.setBackground(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.3f, 1)));

        // Placeholder initial
        Label emptyLabel = new Label("En attente de joueurs...", skin);
        emptyLabel.setFontScale(0.8f);
        playerListTable.add(emptyLabel).pad(10);

        mainTable.add(playerListTable).colspan(5).width(600).height(100).padBottom(20);
        mainTable.row();

        // ==========================================
        // SECTION 2 : CHOIX DU PERSONNAGE
        // ==========================================
        Label charLabel = new Label("Choix du personnage:", skin);
        charLabel.setFontScale(1.5f);
        mainTable.add(charLabel).colspan(5).padBottom(15);
        mainTable.row();

        // Boutons de personnage
        String[] characters = {"beige", "green", "pink", "purple", "yellow"};
        String[] labels = {"Beige", "Vert", "Rose", "Violet", "Jaune"};

        for (int i = 0; i < characters.length; i++) {
            String character = characters[i];
            TextButton button = new TextButton(labels[i], skin);

            if (character.equals("beige")) {
                button.setColor(1, 1, 0.5f, 1);
            }

            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectCharacter(character);
                }
            });

            characterButtons.put(character, button);
            mainTable.add(button).width(100).height(60).pad(5);
        }
        mainTable.row();

        // ESPACE
        mainTable.add().height(20).colspan(5);
        mainTable.row();

        // ==========================================
        // SECTION 3 : BOUTONS CONNEXION
        // ==========================================

        hostButton = new TextButton("Heberger une partie", skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hostGame();
            }
        });
        mainTable.add(hostButton).width(250).height(60).padBottom(20).colspan(5);
        mainTable.row();

        Label orLabel = new Label("--- OU ---", skin);
        orLabel.setFontScale(1.2f);
        mainTable.add(orLabel).colspan(5).padBottom(15);
        mainTable.row();

        ipField = new TextField("localhost", skin);
        ipField.setMessageText("Adresse IP du serveur");
        mainTable.add(ipField).width(250).height(50).padBottom(20).colspan(5);
        mainTable.row();

        joinButton = new TextButton("Rejoindre une partie", skin);
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinGame();
            }
        });
        mainTable.add(joinButton).width(250).height(60).colspan(5);
    }

    /**
     * ✅ NOUVEAU : METTRE À JOUR LA LISTE DES JOUEURS
     */
    private void updatePlayerList() {
        playerListTable.clear();

        if (lobbyPlayers.isEmpty()) {
            Label emptyLabel = new Label("En attente de joueurs...", skin);
            emptyLabel.setFontScale(0.8f);
            playerListTable.add(emptyLabel).pad(10);
        } else {
            // En-tête
            playerListTable.add(new Label("ID", skin)).width(50).padRight(10);
            playerListTable.add(new Label("Nom", skin)).width(150).padRight(10);
            playerListTable.add(new Label("Personnage", skin)).width(150).padRight(10);
            playerListTable.add(new Label("Statut", skin)).width(100);
            playerListTable.row();

            // Ligne de séparation
            playerListTable.add(new Label("---", skin)).colspan(4).padBottom(5);
            playerListTable.row();

            // Chaque joueur
            for (LobbyStateMessage.LobbyPlayerData player : lobbyPlayers.values()) {
                // ID
                Label idLabel = new Label(String.valueOf(player.playerId), skin);
                playerListTable.add(idLabel).width(50).padRight(10);

                // Nom
                Label nameLabel = new Label(player.playerName, skin);
                playerListTable.add(nameLabel).width(150).padRight(10);

                // Personnage
                String characterName = getCharacterDisplayName(player.characterType);
                Label charLabel = new Label(characterName, skin);
                charLabel.setColor(getCharacterColor(player.characterType));
                playerListTable.add(charLabel).width(150).padRight(10);

                // Statut (Host ou Client)
                Label statusLabel = new Label(player.isHost ? "[HOST]" : "[Client]", skin);
                statusLabel.setColor(player.isHost ? Color.GOLD : Color.LIGHT_GRAY);
                playerListTable.add(statusLabel).width(100);

                playerListTable.row();
            }
        }
    }

    /**
     * OBTENIR LE NOM D'AFFICHAGE DU PERSONNAGE
     */
    private String getCharacterDisplayName(String characterType) {
        switch (characterType) {
            case "beige": return "Beige";
            case "green": return "Vert";
            case "pink": return "Rose";
            case "purple": return "Violet";
            case "yellow": return "Jaune";
            default: return characterType;
        }
    }

    /**
     * OBTENIR LA COULEUR D'AFFICHAGE DU PERSONNAGE
     */
    private Color getCharacterColor(String characterType) {
        switch (characterType) {
            case "beige": return new Color(0.8f, 0.7f, 0.5f, 1);
            case "green": return new Color(0.3f, 0.8f, 0.3f, 1);
            case "pink": return new Color(1.0f, 0.6f, 0.8f, 1);
            case "purple": return new Color(0.7f, 0.3f, 0.9f, 1);
            case "yellow": return new Color(1.0f, 0.9f, 0.3f, 1);
            default: return Color.WHITE;
        }
    }

    private void selectCharacter(String character) {
        for (TextButton button : characterButtons.values()) {
            button.setColor(Color.WHITE);
        }

        TextButton selected = characterButtons.get(character);
        if (selected != null) {
            selected.setColor(1, 1, 0.5f, 1);
        }

        selectedCharacter = character;
        System.out.println("Personnage sélectionné : " + character);

        // Envoyer le changement si connecté
        if (networkManager.isConnected()) {
            networkManager.sendCharacterChoice(selectedCharacter);
        }
    }

    private void hostGame() {
        statusMessage = "Demarrage du serveur...";
        hostButton.setDisabled(true);
        joinButton.setDisabled(true);

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

        networkManager.setLocalCharacter(selectedCharacter);
        connectionCheckStarted = true;
        networkManager.connectToHost(ip);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Vérifier connexion
        if (connectionCheckStarted && networkManager.isConnected() && !gameScreenCreated) {
            gameScreenCreated = true;

            System.out.println("✅ [LOBBY] Connexion détectée, création GameScreen unique");
            statusMessage = "Connecte! Chargement du jeu...";
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

    // ✅ NOUVEAU : RÉCEPTION DE L'ÉTAT DU LOBBY
    @Override
    public void onLobbyStateReceived(LobbyStateMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("📋 [LOBBY] État reçu : " + message.players.size() + " joueurs");

            lobbyPlayers.clear();
            lobbyPlayers.putAll(message.players);

            updatePlayerList();
        });
    }

    @Override
    public void onConnectedToServer() {}

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
            lobbyPlayers.clear();
            updatePlayerList();
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
    public void onPlayerCharacterChanged(PlayerCharacterMessage message) {}

    private Skin getSkin() {
        Skin skin = new Skin();

        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        buttonStyle.down = skin.newDrawable("white", Color.GRAY);
        buttonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        buttonStyle.font = skin.getFont("default-font");
        skin.add("default", buttonStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default-font");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", Color.BLUE);
        skin.add("default", textFieldStyle);

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
