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



    // INTERFACE - Section lobby (visible après connexion)
    private Table lobbySection;         // Section lobby complète (cachée au début)
    private Table connectionSection;    // Section connexion (cachée après connexion)
    private Table playerListTable;      // Liste des joueurs
    private TextButton readyButton;     // Bouton Prêt / Pas Prêt
    private Label allReadyLabel;        // Message "En attente de joueurs..."

    // DONNÉES
    private Map<Integer, LobbyStateMessage.LobbyPlayerData> lobbyPlayers;
    private boolean isLocalReady = false;
    private boolean allPlayersReady = false;
    private boolean isConnectedToServer = false;

    // Flag connexion
    private boolean connectionCheckStarted = false;
    private boolean gameScreenCreated = false;
    private boolean gameStartSignalReceived = false; // signal de lancement

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
        Table root = new Table();
        root.setFillParent(true);
        root.padTop(15).padBottom(15);
        stage.addActor(root);

        // ==========================================
        // TITRE
        // ==========================================
        Label titleLabel = new Label("Kawaii Verse Coop", skin);
        titleLabel.setFontScale(2.0f);
        titleLabel.setColor(Color.CYAN);
        root.add(titleLabel).colspan(5).padBottom(15);
        root.row();

        // ==========================================
        // SECTION CHOIX DE PERSONNAGE (toujours visible)
        // ==========================================
        Label charLabel = new Label("Choix du personnage:", skin);
        charLabel.setFontScale(1.3f);
        root.add(charLabel).colspan(5).padBottom(8);
        root.row();

        String[] characters = {"beige", "green", "pink", "purple", "yellow"};
        String[] charLabels  = {"Beige", "Vert",  "Rose", "Violet", "Jaune"};

        for (int i = 0; i < characters.length; i++) {
            final String character = characters[i];
            TextButton btn = new TextButton(charLabels[i], skin);
            if (character.equals("beige")) btn.setColor(1, 1, 0.5f, 1);

            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectCharacter(character);
                }
            });

            characterButtons.put(character, btn);
            root.add(btn).width(100).height(50).pad(4);
        }
        root.row();

        // Séparateur
        root.add().height(10).colspan(5);
        root.row();

        // ==========================================
        // SECTION CONNEXION (visible avant connexion)
        // ==========================================
        connectionSection = new Table();

        hostButton = new TextButton("Heberger une partie", skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hostGame();
            }
        });
        connectionSection.add(hostButton).width(250).height(55).padBottom(10);
        connectionSection.row();

        Label orLabel = new Label("--- OU ---", skin);
        connectionSection.add(orLabel).padBottom(8);
        connectionSection.row();

        ipField = new TextField("localhost", skin);
        ipField.setMessageText("Adresse IP du serveur");
        connectionSection.add(ipField).width(250).height(45).padBottom(8);
        connectionSection.row();

        joinButton = new TextButton("Rejoindre une partie", skin);
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinGame();
            }
        });
        connectionSection.add(joinButton).width(250).height(55);

        root.add(connectionSection).colspan(5).padBottom(10);
        root.row();

        // ==========================================
        // SECTION LOBBY (visible après connexion)
        // ==========================================
        lobbySection = new Table();
        lobbySection.setVisible(false); // Cachée au départ

        // Liste des joueurs
        Label playersTitle = new Label("Joueurs dans le lobby:", skin);
        playersTitle.setFontScale(1.3f);
        lobbySection.add(playersTitle).padBottom(8);
        lobbySection.row();

        playerListTable = new Table();
        playerListTable.setBackground(skin.newDrawable("white", new Color(0.15f, 0.15f, 0.25f, 1)));
        updatePlayerList(); // Initialiser vide

        // Table directement, pas de ScrollPane
        lobbySection.add(playerListTable).width(550).padBottom(12);
        lobbySection.row();

        // Message statut "tous prêts"
        allReadyLabel = new Label("En attente que tous les joueurs soient prets...", skin);
        allReadyLabel.setColor(Color.ORANGE);
        lobbySection.add(allReadyLabel).padBottom(10);
        lobbySection.row();

        // Bouton Ready
        readyButton = new TextButton("  Je suis PRET !  ", skin);
        readyButton.setColor(new Color(0.2f, 0.7f, 0.2f, 1)); // Vert
        readyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleReady();
            }
        });
        lobbySection.add(readyButton).width(220).height(60);

        root.add(lobbySection).colspan(5);
        root.row();
    }

    /**
     * BASCULER LE STATUT PRÊT
     */
    private void toggleReady() {
        isLocalReady = !isLocalReady;

        // Mettre à jour l'apparence du bouton
        if (isLocalReady) {
            readyButton.setText("  Pas encore pret...  ");
            readyButton.setColor(new Color(0.7f, 0.2f, 0.2f, 1)); // Rouge = "cliquer pour annuler"
        } else {
            readyButton.setText("  Je suis PRET !  ");
            readyButton.setColor(new Color(0.2f, 0.7f, 0.2f, 1)); // Vert
        }

        // Envoyer au serveur
        networkManager.sendReadyState(isLocalReady);
        System.out.println("[LOBBY] Statut prêt: " + isLocalReady);
    }

    /**
     * METTRE À JOUR LA LISTE DES JOUEURS
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

        // Lancer le jeu quand signal reçu
        if (gameStartSignalReceived && !gameScreenCreated) {
            gameScreenCreated = true;
            System.out.println("[LOBBY] Signal reçu → Lancement GameScreen !");
            game.setScreen(new GameScreen(game, networkManager, selectedCharacter));
            return;
        }

        // Afficher la section lobby après connexion
        if (connectionCheckStarted && networkManager.isConnected() && !isConnectedToServer) {
            isConnectedToServer = true;
            showLobbySection();
        }

        stage.act(delta);
        stage.draw();

        // Message de statut en bas
        batch.begin();
        font.draw(batch, statusMessage, 50, 30);
        batch.end();
    }

    /**
     * AFFICHER LA SECTION LOBBY (après connexion)
     */
    private void showLobbySection() {
        connectionSection.setVisible(false);
        lobbySection.setVisible(true);
        statusMessage = "Connecte ! Choisis ton personnage et clique sur Pret.";
        System.out.println("[LOBBY] Section lobby affichée");
    }

    // RÉCEPTION DE L'ÉTAT DU LOBBY
    @Override
    public void onLobbyStateReceived(LobbyStateMessage message) {
        Gdx.app.postRunnable(() -> {
            lobbyPlayers.clear();
            // Mettre le host en premier
            LobbyStateMessage.LobbyPlayerData hostData = null;
            for (LobbyStateMessage.LobbyPlayerData player : message.players.values()) {
                if (player.isHost) hostData = player;
            }
            if (hostData != null) lobbyPlayers.put(hostData.playerId, hostData);
            for (LobbyStateMessage.LobbyPlayerData player : message.players.values()) {
                if (!player.isHost) lobbyPlayers.put(player.playerId, player);
            }

            allPlayersReady = message.allPlayersReady;
            updatePlayerList();

            // Mettre à jour le message de statut "tous prêts"
            if (allReadyLabel != null) {
                if (allPlayersReady) {
                    allReadyLabel.setText("Tous les joueurs sont prets ! Lancement imminent...");
                    allReadyLabel.setColor(Color.GREEN);
                } else {
                    allReadyLabel.setText("En attente que tous les joueurs soient prets...");
                    allReadyLabel.setColor(Color.ORANGE);
                }
            }
        });
    }

    // Réception du statut ready (routé depuis onLobbyStateReceived)
    @Override
    public void onPlayerReadyStateReceived(LobbyStateMessage message) {
        // Déjà géré dans onLobbyStateReceived, rien de plus à faire ici
    }

    // Signal de lancement de partie
    @Override
    public void onGameStartReceived() {
        Gdx.app.postRunnable(() -> {
            System.out.println("[LOBBY] onGameStartReceived → Transition vers GameScreen");
            gameStartSignalReceived = true;
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
