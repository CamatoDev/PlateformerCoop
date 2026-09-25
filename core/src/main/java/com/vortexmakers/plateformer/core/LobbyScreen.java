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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.vortexmakers.plateformer.network.NetworkManager;
import com.vortexmakers.plateformer.network.listeners.NetworkListener;
import com.vortexmakers.plateformer.network.messages.*;
import com.vortexmakers.plateformer.utils.FontManager;

import java.util.HashMap;
import java.util.Map;

/**
 * LOBBYSCREEN - Sélection de personnage et connexion multijoueur.
 * Layout ultra-compact pour ne rien masquer en bas.
 */
public class LobbyScreen implements Screen, NetworkListener {

    private final PlateformerGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Stage stage;
    private Skin skin;

    private NetworkManager networkManager;

    private TextField ipField;
    private TextButton hostButton;
    private TextButton joinButton;
    private String statusMessage = "Pret a jouer";

    private String selectedCharacter = "beige";
    private Map<String, TextButton> characterButtons = new HashMap<>();

    private Table connectionSection;
    private Table lobbySection;
    private Table playerListTable;
    private TextButton readyButton;
    private Label allReadyLabel;

    private Map<Integer, LobbyStateMessage.LobbyPlayerData> lobbyPlayers = new HashMap<>();
    private boolean isLocalReady      = false;
    private boolean allPlayersReady   = false;
    private boolean isConnectedToServer = false;
    private boolean connectionCheckStarted = false;
    private boolean gameScreenCreated = false;
    private boolean gameStartSignalReceived = false;

    private Texture backgroundTexture;
    private Map<String, Texture> characterPortraits = new HashMap<>();

    public static String lastIp = "localhost";

    private static final Color C_BEIGE  = new Color(0.88f, 0.73f, 0.50f, 1f);
    private static final Color C_GREEN  = new Color(0.45f, 0.82f, 0.45f, 1f);
    private static final Color C_PINK   = new Color(1.00f, 0.62f, 0.73f, 1f);
    private static final Color C_PURPLE = new Color(0.74f, 0.58f, 0.95f, 1f);
    private static final Color C_YELLOW = new Color(1.00f, 0.88f, 0.28f, 1f);

    private static final int VW = 1080, VH = 720;
    private static final int CHAR_BTN_W = 148, CHAR_BTN_H = 50;
    private static final int ACTION_BTN_W = 270, ACTION_BTN_H = 56;
    private static final int READY_BTN_W  = 250, READY_BTN_H = 56;
    private static final int IP_FIELD_H   = 44;
    private static final int PORTRAIT_SIZE = 46;
    private static final int BACK_BTN_W   = 190, BACK_BTN_H = 44;

    public LobbyScreen(PlateformerGame game) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.networkManager.setNetworkListener(this);
    }

    private Texture roundRect(int w, int h, Color color) {
        Pixmap px = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        px.setColor(color);
        int r = Math.min(h / 2, 22);
        px.fillRectangle(r, 0, w - 2*r, h);
        px.fillRectangle(0, r, w, h - 2*r);
        px.fillCircle(r, r, r);      px.fillCircle(w-r, r, r);
        px.fillCircle(r, h-r, r);   px.fillCircle(w-r, h-r, r);
        Texture t = new Texture(px); px.dispose(); return t;
    }

    private TextButton.TextButtonStyle btnStyle(BitmapFont font, Color up, Color hover) {
        Texture tUp  = roundRect(ACTION_BTN_W, ACTION_BTN_H, up);
        Texture tHov = roundRect(ACTION_BTN_W, ACTION_BTN_H, hover);
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font; s.fontColor = Color.WHITE;
        s.up   = new TextureRegionDrawable(new TextureRegion(tUp));
        s.over = new TextureRegionDrawable(new TextureRegion(tHov));
        s.down = new TextureRegionDrawable(new TextureRegion(tUp));
        return s;
    }

    private Color darken(Color c, float f) {
        return new Color(c.r*f, c.g*f, c.b*f, 1f);
    }

    private Color lighten(Color c, float f) {
        return new Color(Math.min(1f, c.r+f), Math.min(1f, c.g+f), Math.min(1f, c.b+f), 1f);
    }

    private Color charColor(String ch) {
        switch (ch) {
            case "beige":  return C_BEIGE;
            case "green":  return C_GREEN;
            case "pink":   return C_PINK;
            case "purple": return C_PURPLE;
            case "yellow": return C_YELLOW;
            default: return Color.WHITE;
        }
    }

    @Override
    public void show() {
        FontManager.getInstance().load();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, VW, VH);
        batch  = new SpriteBatch();

        backgroundTexture = new Texture(Gdx.files.internal("Backgrounds/background_color_hills.png"));
        loadPortraits();

        skin  = buildSkin();
        stage = new Stage(new FitViewport(VW, VH));
        Gdx.input.setInputProcessor(stage);

        buildUI();
        updateCharacterButtonsVisuals(); // Mets en surbrillance le perso par défaut
    }

    private void loadPortraits() {
        for (String c : new String[]{"beige","green","pink","purple","yellow"}) {
            String path = "Characters/character_" + c + "_idle.png";
            if (Gdx.files.internal(path).exists())
                characterPortraits.put(c, new Texture(Gdx.files.internal(path)));
        }
    }

    private Skin buildSkin() {
        Skin s = new Skin();
        BitmapFont bodyFont   = FontManager.getInstance().getBody();
        BitmapFont smallFont  = FontManager.getInstance().getSmall();

        Pixmap wpx = new Pixmap(1,1,Pixmap.Format.RGBA8888);
        wpx.setColor(Color.WHITE); wpx.fill();
        s.add("white", new Texture(wpx)); wpx.dispose();

        // TextField avec police smallFont pour ne pas déborder (44px)
        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font       = smallFont; 
        tfs.fontColor  = Color.WHITE;
        tfs.background = s.newDrawable("white", new Color(0.13f, 0.12f, 0.26f, 0.95f));
        tfs.cursor     = s.newDrawable("white", Color.WHITE);
        tfs.selection  = s.newDrawable("white", new Color(0.5f,0.5f,0.9f,0.5f));
        s.add("default", tfs);

        Label.LabelStyle ls = new Label.LabelStyle(bodyFont, Color.WHITE);
        s.add("default", ls);

        return s;
    }

    private void buildUI() {
        FontManager fm = FontManager.getInstance();
        BitmapFont subtitleFont = fm.getSubtitle();
        BitmapFont bodyFont     = fm.getBody();
        BitmapFont smallFont    = fm.getSmall();

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(0); // Réduction de la marge du haut
        stage.addActor(root);

        // TITRE
        Label.LabelStyle titleStyle = new Label.LabelStyle(subtitleFont, new Color(0.79f,0.72f,1f,1f));
        Label titleLabel = new Label("Kawaii Verse", titleStyle);
        root.add(titleLabel).colspan(5).padBottom(2);
        root.row();

        // LABEL CHOIX PERSONNAGE
        Label.LabelStyle subStyle = new Label.LabelStyle(smallFont, new Color(0.25f,0.15f,0.38f,1f));
        root.add(new Label("Choix du personnage:", subStyle)).colspan(5).padBottom(2);
        root.row();

        // BOUTONS PERSONNAGE
        String[] keys    = {"beige","green","pink","purple","yellow"};
        String[] labels  = {"Beige","Vert","Rose","Violet","Jaune"};

        for (int i = 0; i < keys.length; i++) {
            final String ch = keys[i];
            Color base = charColor(ch);
            Texture tUp  = roundRect(CHAR_BTN_W, CHAR_BTN_H, base);
            Texture tHov = roundRect(CHAR_BTN_W, CHAR_BTN_H, lighten(base, 0.1f));
            Texture tDwn = roundRect(CHAR_BTN_W, CHAR_BTN_H, darken(base, 0.75f));

            TextButton.TextButtonStyle cs = new TextButton.TextButtonStyle();
            cs.font = smallFont; cs.fontColor = Color.WHITE;
            cs.up      = new TextureRegionDrawable(new TextureRegion(tUp));
            cs.over    = new TextureRegionDrawable(new TextureRegion(tHov));
            cs.down    = new TextureRegionDrawable(new TextureRegion(tDwn));
            cs.checked = new TextureRegionDrawable(new TextureRegion(tDwn)); // État sélectionné

            TextButton btn = new TextButton(labels[i], cs);
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    selectCharacter(ch);
                }
            });
            characterButtons.put(ch, btn);
            root.add(btn).width(CHAR_BTN_W).height(CHAR_BTN_H).pad(2);
        }
        root.row();

        // PORTRAITS
        for (String ch : keys) {
            Texture p = characterPortraits.get(ch);
            if (p != null) {
                root.add(new Image(new TextureRegion(p))).width(PORTRAIT_SIZE).height(PORTRAIT_SIZE);
            } else {
                root.add().width(PORTRAIT_SIZE).height(PORTRAIT_SIZE);
            }
        }
        root.row().padTop(2);

        // SECTION CONNEXION
        connectionSection = new Table();

        hostButton = new TextButton("Heberger une partie",
            btnStyle(smallFont, new Color(0.79f,0.72f,1f,1f), new Color(0.89f,0.82f,1f,1f)));
        hostButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { hostGame(); }
        });
        connectionSection.add(hostButton).width(ACTION_BTN_W).height(ACTION_BTN_H).padBottom(4);
        connectionSection.row();

        Label.LabelStyle orStyle = new Label.LabelStyle(smallFont, new Color(0.35f,0.25f,0.5f,1f));
        connectionSection.add(new Label("--- OU ---", orStyle)).padBottom(4);
        connectionSection.row();

        ipField = new TextField(lastIp, skin);
        connectionSection.add(ipField).width(ACTION_BTN_W).height(IP_FIELD_H).padBottom(4);
        connectionSection.row();

        joinButton = new TextButton("Rejoindre une partie",
            btnStyle(smallFont, new Color(1f,0.71f,0.78f,1f), new Color(1f,0.85f,0.91f,1f)));
        joinButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { joinGame(); }
        });
        connectionSection.add(joinButton).width(ACTION_BTN_W).height(ACTION_BTN_H).padBottom(6);
        connectionSection.row();

        // Bouton Retour
        Texture backTex = roundRect(BACK_BTN_W, BACK_BTN_H, new Color(0.65f,0.65f,0.75f,0.92f));
        Texture backHov = roundRect(BACK_BTN_W, BACK_BTN_H, new Color(0.78f,0.78f,0.88f,1f));
        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();
        backStyle.font = smallFont; backStyle.fontColor = Color.WHITE;
        backStyle.up   = new TextureRegionDrawable(new TextureRegion(backTex));
        backStyle.over = new TextureRegionDrawable(new TextureRegion(backHov));
        backStyle.down = new TextureRegionDrawable(new TextureRegion(backTex));

        TextButton backBtn = new TextButton("< Retour au menu", backStyle);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                networkManager.setNetworkListener(null);
                if (networkManager.isConnected()) networkManager.disconnect();
                game.setScreen(new TitleScreen(game));
            }
        });
        connectionSection.add(backBtn).width(BACK_BTN_W).height(BACK_BTN_H);

        root.add(connectionSection).colspan(5).padBottom(2);
        root.row();

        // SECTION LOBBY
        lobbySection = new Table();
        lobbySection.setVisible(false);

        Label.LabelStyle lobbyTitleStyle = new Label.LabelStyle(bodyFont, new Color(0.2f,0.12f,0.35f,1f));
        lobbySection.add(new Label("Joueurs dans le lobby:", lobbyTitleStyle)).padBottom(2);
        lobbySection.row();

        playerListTable = new Table();
        Pixmap tableBg = new Pixmap(550, 130, Pixmap.Format.RGBA8888);
        tableBg.setColor(new Color(0.10f, 0.08f, 0.20f, 0.88f)); tableBg.fill();
        playerListTable.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(tableBg))));
        tableBg.dispose();
        playerListTable.pad(4);
        updatePlayerList();
        lobbySection.add(playerListTable).width(550).height(130).padBottom(4);
        lobbySection.row();

        allReadyLabel = new Label("En attente que tous les joueurs soient prets...", skin);
        allReadyLabel.setStyle(new Label.LabelStyle(smallFont, Color.ORANGE));
        allReadyLabel.setWrap(false);
        lobbySection.add(allReadyLabel).padBottom(4);
        lobbySection.row();

        Texture rGreen = roundRect(READY_BTN_W, READY_BTN_H, new Color(0.50f,0.85f,0.50f,1f));
        Texture rGHov  = roundRect(READY_BTN_W, READY_BTN_H, new Color(0.65f,0.93f,0.65f,1f));
        TextButton.TextButtonStyle readyStyle = new TextButton.TextButtonStyle();
        readyStyle.font = smallFont; readyStyle.fontColor = Color.WHITE;
        readyStyle.up   = new TextureRegionDrawable(new TextureRegion(rGreen));
        readyStyle.over = new TextureRegionDrawable(new TextureRegion(rGHov));
        readyStyle.down = new TextureRegionDrawable(new TextureRegion(rGreen));

        readyButton = new TextButton("Je suis PRET !", readyStyle);
        readyButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { toggleReady(); }
        });
        lobbySection.add(readyButton).width(READY_BTN_W).height(READY_BTN_H);

        root.add(lobbySection).colspan(5);
    }

    private void selectCharacter(String ch) {
        selectedCharacter = ch;
        System.out.println("Personnage: " + ch);
        if (networkManager.isConnected()) networkManager.sendCharacterChoice(ch);
        updateCharacterButtonsVisuals();
    }

    private void updateCharacterButtonsVisuals() {
        for (Map.Entry<String, TextButton> entry : characterButtons.entrySet()) {
            TextButton btn = entry.getValue();
            btn.setChecked(entry.getKey().equals(selectedCharacter));
        }
    }

    private void toggleReady() {
        isLocalReady = !isLocalReady;
        BitmapFont sf = FontManager.getInstance().getSmall();

        if (isLocalReady) {
            readyButton.setText("Pas encore pret...");
            Texture tRed = roundRect(READY_BTN_W, READY_BTN_H, new Color(1f,0.52f,0.52f,1f));
            TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
            s.font = sf; s.fontColor = Color.WHITE;
            s.up = s.over = s.down = new TextureRegionDrawable(new TextureRegion(tRed));
            readyButton.setStyle(s);
        } else {
            readyButton.setText("Je suis PRET !");
            Texture tGrn = roundRect(READY_BTN_W, READY_BTN_H, new Color(0.50f,0.85f,0.50f,1f));
            TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
            s.font = sf; s.fontColor = Color.WHITE;
            s.up = s.over = s.down = new TextureRegionDrawable(new TextureRegion(tGrn));
            readyButton.setStyle(s);
        }
        networkManager.sendReadyState(isLocalReady);
    }

    private void updatePlayerList() {
        playerListTable.clear();
        BitmapFont sf = FontManager.getInstance().getSmall();

        if (lobbyPlayers.isEmpty()) {
            Label.LabelStyle es = new Label.LabelStyle(sf, new Color(0.8f,0.8f,1f,1f));
            playerListTable.add(new Label("En attente de joueurs...", es)).pad(8);
        } else {
            Label.LabelStyle hs = new Label.LabelStyle(sf, new Color(0.75f,0.75f,1f,1f));
            playerListTable.add(new Label("Nom", hs)).width(160).padRight(10);
            playerListTable.add(new Label("Personnage", hs)).width(150).padRight(10);
            playerListTable.add(new Label("Statut", hs)).width(110);
            playerListTable.row().padBottom(4);

            for (LobbyStateMessage.LobbyPlayerData p : lobbyPlayers.values()) {
                String nm = p.playerName + (p.isHost ? " [H]" : "");
                Label.LabelStyle ws = new Label.LabelStyle(sf, Color.WHITE);
                playerListTable.add(new Label(nm, ws)).width(160).padRight(10);

                Label.LabelStyle cs = new Label.LabelStyle(sf, charColor(p.characterType));
                playerListTable.add(new Label(charDisplayName(p.characterType), cs)).width(150).padRight(10);

                Color sc = p.isReady ? new Color(0.4f,0.9f,0.4f,1f) : Color.ORANGE;
                Label.LabelStyle ss = new Label.LabelStyle(sf, sc);
                playerListTable.add(new Label(p.isReady ? "PRET" : "Attente", ss)).width(110);
                playerListTable.row().padTop(3);
            }
        }
    }

    private String charDisplayName(String t) {
        switch (t) {
            case "beige": return "Beige";  case "green":  return "Vert";
            case "pink":  return "Rose";   case "purple": return "Violet";
            case "yellow":return "Jaune";  default: return t;
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
                    connectionCheckStarted = true;
                    networkManager.connectToHost("localhost");
                });
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void joinGame() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) { statusMessage = "Entrez une adresse IP"; return; }
        lastIp = ip;
        statusMessage = "Connexion a " + ip + "...";
        hostButton.setDisabled(true);
        joinButton.setDisabled(true);
        networkManager.setLocalCharacter(selectedCharacter);
        connectionCheckStarted = true;
        networkManager.connectToHost(ip);
    }

    private void showLobbySection() {
        connectionSection.setVisible(false);
        lobbySection.setVisible(true);
        if (networkManager.isHost()) {
            try {
                String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                statusMessage = "Votre IP : " + ip + "  Donnez-la a votre ami !";
            } catch (Exception e) {
                statusMessage = "Hote — Partagez votre IP locale";
            }
        } else {
            statusMessage = "Connecte ! Choisis ton perso et clique Pret.";
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.88f, 0.94f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (gameStartSignalReceived && !gameScreenCreated) {
            gameScreenCreated = true;
            game.setScreen(new GameScreen(game, networkManager, selectedCharacter));
            return;
        }

        if (connectionCheckStarted && networkManager.isConnected() && !isConnectedToServer) {
            isConnectedToServer = true;
            showLobbySection();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, VW, VH);
        batch.end();

        stage.act(delta);
        stage.draw();

        batch.begin();
        BitmapFont sf = FontManager.getInstance().getSmall();
        sf.setColor(new Color(0.15f, 0.08f, 0.30f, 1f));
        sf.draw(batch, statusMessage, 18, 26);
        batch.end();
    }

    @Override
    public void onLobbyStateReceived(LobbyStateMessage message) {
        Gdx.app.postRunnable(() -> {
            lobbyPlayers.clear();
            LobbyStateMessage.LobbyPlayerData host = null;
            for (LobbyStateMessage.LobbyPlayerData p : message.players.values())
                if (p.isHost) host = p;
            if (host != null) lobbyPlayers.put(host.playerId, host);
            for (LobbyStateMessage.LobbyPlayerData p : message.players.values())
                if (!p.isHost) lobbyPlayers.put(p.playerId, p);

            allPlayersReady = message.allPlayersReady;
            updatePlayerList();

            if (allReadyLabel != null) {
                BitmapFont sf = FontManager.getInstance().getSmall();
                if (allPlayersReady) {
                    allReadyLabel.setText("Tous prets ! Lancement imminent...");
                    allReadyLabel.setStyle(new Label.LabelStyle(sf, new Color(0.4f,0.9f,0.4f,1f)));
                } else {
                    allReadyLabel.setText("En attente que tous les joueurs soient prets...");
                    allReadyLabel.setStyle(new Label.LabelStyle(sf, Color.ORANGE));
                }
            }
        });
    }

    @Override public void onPlayerReadyStateReceived(LobbyStateMessage message) {}
    @Override public void onGameStartReceived() {
        Gdx.app.postRunnable(() -> gameStartSignalReceived = true);
    }
    @Override public void onConnectedToServer() {}
    @Override public void onPlayerJoined(PlayerJoinMessage message) {}
    @Override public void onPlayerLeft(PlayerLeaveMessage message) {}
    @Override public void onGameStateReceived(GameStateMessage message) {}
    @Override public void onDisconnectedFromServer() {
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
    @Override public void onConnectionFailed(String reason) {
        Gdx.app.postRunnable(() -> {
            statusMessage = "Erreur: " + reason;
            hostButton.setDisabled(false);
            joinButton.setDisabled(false);
            connectionCheckStarted = false;
            gameScreenCreated = false;
        });
    }
    @Override public void onPlatformStateReceived(PlatformStateMessage message) {}
    @Override public void onCollectibleStateReceived(CollectibleStateMessage message) {}
    @Override public void onGameTimerReceived(GameTimerMessage message) {}
    @Override public void onFinishFlagStateReceived(FinishFlagStateMessage message) {}
    @Override public void onSpikeStateReceived(SpikeStateMessage message) {}
    @Override public void onPlayerRespawned(PlayerRespawnMessage message) {}
    @Override public void onPlayerCharacterChanged(PlayerCharacterMessage message) {}

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        for (Texture t : characterPortraits.values()) if (t != null) t.dispose();
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        if (skin  != null) skin.dispose();
    }
}
