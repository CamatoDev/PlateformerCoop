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
import com.vortexmakers.plateformer.utils.FontManager;

import java.util.*;

/**
 * GAMEOVERSCREEN - Affichage fin de partie (temps écoulé).
 *
 * Layout :
 *  - Titre "TEMPS ECOULE !" en haut (hors panneau)
 *  - Sous-titre "Vous n'avez pas terminé à temps..." (plus bas, hors panneau)
 *  - Panneau central arrondi : UNIQUEMENT la section scores en mode PODIUM classé
 *  - Boutons "Retour au menu" / "Réessayer" EN DESSOUS du panneau
 */
public class GameOverScreen implements Screen {

    // ============================================================
    // CHAMPS
    // ============================================================
    private final PlateformerGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Stage stage;
    private GlyphLayout layout;

    // Polices (non-owned — gérées par FontManager)
    private BitmapFont titleFont;
    private BitmapFont smallFont;

    // Textures
    private Texture background;
    private Texture panelTex;
    private Texture btnMenuTex, btnMenuHovTex;
    private Texture btnRetryTex;

    // Données
    private final float timeElapsed;
    private NetworkManager networkManager;
    private java.util.List<PlayerScore> rankedPlayers;

    private static final int VW = 1080;
    private static final int VH = 720;

    // Dimensions du panneau (scores seulement)
    private int panelW = 680;
    private int panelH = 180;
    private float panelX, panelY;

    // Positions fixes
    private static final float TITLE_Y   = VH - 50f;   // Titre remonté un peu
    private static final float SUB_Y     = VH - 120f;  // Sous-titre décalé
    private static final float BTN_Y     = 75f;

    // Couleurs de rang
    private static final Color COLOR_GOLD   = new Color(1.00f, 0.84f, 0.00f, 1f);
    private static final Color COLOR_SILVER = new Color(0.80f, 0.80f, 0.80f, 1f);
    private static final Color COLOR_BRONZE = new Color(0.85f, 0.52f, 0.22f, 1f);
    private static final Color COLOR_OTHER  = new Color(0.92f, 0.92f, 0.92f, 1f);

    // ============================================================
    // CLASSE INTERNE
    // ============================================================
    private static class PlayerScore implements Comparable<PlayerScore> {
        int playerId;
        String playerName;
        int score;

        PlayerScore(int id, String name, int score) {
            this.playerId   = id;
            this.playerName = name;
            this.score      = score;
        }

        @Override
        public int compareTo(PlayerScore other) {
            return Integer.compare(other.score, this.score);
        }
    }

    // ============================================================
    // CONSTRUCTEUR
    // ============================================================
    public GameOverScreen(PlateformerGame game, int localPlayerScore,
                          Map<Integer, Integer> remotePlayerScores, float timeElapsed) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.timeElapsed = timeElapsed;

        Map<Integer, PlayerScore> playerScoresMap = new HashMap<>();

        int localId = networkManager.getLocalPlayerId();
        playerScoresMap.put(localId, new PlayerScore(
            localId,
            networkManager.isHost() ? "Vous (Hote)" : "Vous",
            localPlayerScore
        ));

        for (Map.Entry<Integer, Integer> e : remotePlayerScores.entrySet()) {
            playerScoresMap.put(e.getKey(), new PlayerScore(
                e.getKey(), "Joueur " + e.getKey(), e.getValue()
            ));
        }

        rankedPlayers = new ArrayList<>(playerScoresMap.values());
        Collections.sort(rankedPlayers);
        
        System.out.println("GameOverScreen créé - Temps: " + formatTime((int) timeElapsed));
    }

    // ============================================================
    // HELPER GRAPHIQUE
    // ============================================================
    private Texture roundRect(int w, int h, Color color) {
        Pixmap px = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        px.setColor(color);
        int r = Math.min(h / 2, 24);
        px.fillRectangle(r, 0, w - 2*r, h);
        px.fillRectangle(0, r, w, h - 2*r);
        px.fillCircle(r, r, r);      px.fillCircle(w-r, r, r);
        px.fillCircle(r, h-r, r);   px.fillCircle(w-r, h-r, r);
        Texture t = new Texture(px); px.dispose(); return t;
    }

    // ============================================================
    // SHOW
    // ============================================================
    @Override
    public void show() {
        FontManager.getInstance().load();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, VW, VH);
        batch  = new SpriteBatch();
        layout = new GlyphLayout();

        titleFont = FontManager.getInstance().getTitle();
        smallFont = FontManager.getInstance().getSmall(); // Utilisation exclusive pour les textes

        background = new Texture(Gdx.files.internal("Backgrounds/background_color_hills.png"));

        // Hauteur du panneau = header (60) + temps (40) + joueurs (45 * nb) + encouragement (50)
        int maxDisplay = Math.min(rankedPlayers.size(), 5);
        panelH = 60 + 40 + (maxDisplay * 45) + 50;
        panelH = Math.max(panelH, 260); // minimum
        panelH = Math.min(panelH, 420); // maximum

        panelX = (VW - panelW) / 2f;
        float zoneTop    = SUB_Y - 20f;
        float zoneBottom = BTN_Y + 70f + 20f;
        panelY = zoneBottom + (zoneTop - zoneBottom - panelH) / 2f;
        panelY = Math.max(zoneBottom, panelY);

        panelTex      = roundRect(panelW, panelH, new Color(0.22f, 0.07f, 0.15f, 0.93f));
        btnMenuTex    = roundRect(300, 66, new Color(1f, 0.71f, 0.78f, 1f));
        btnMenuHovTex = roundRect(300, 66, new Color(1f, 0.85f, 0.91f, 1f));
        btnRetryTex   = roundRect(300, 66, new Color(0.5f, 0.5f, 0.55f, 1f));

        stage = new Stage(new FitViewport(VW, VH));
        Gdx.input.setInputProcessor(stage);
        createButtonUI();
    }

    private void createButtonUI() {
        // Bouton "Retour au menu" avec police smallFont (pour éviter le débordement)
        TextButton.TextButtonStyle menuStyle = new TextButton.TextButtonStyle();
        menuStyle.font      = smallFont;
        menuStyle.fontColor = Color.WHITE;
        menuStyle.up   = new TextureRegionDrawable(new TextureRegion(btnMenuTex));
        menuStyle.over = new TextureRegionDrawable(new TextureRegion(btnMenuHovTex));
        menuStyle.down = new TextureRegionDrawable(new TextureRegion(btnMenuTex));

        TextButton menuBtn = new TextButton("Retour au menu", menuStyle);
        menuBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { returnToMenu(); }
        });

        // Bouton "Réessayer" (désactivé)
        TextButton.TextButtonStyle retryStyle = new TextButton.TextButtonStyle();
        retryStyle.font      = smallFont;
        retryStyle.fontColor = new Color(0.65f, 0.65f, 0.65f, 1f);
        retryStyle.up = new TextureRegionDrawable(new TextureRegion(btnRetryTex));

        TextButton retryBtn = new TextButton("Reessayer", retryStyle);
        retryBtn.setDisabled(true);

        Table table = new Table();
        table.setFillParent(true);
        table.bottom().padBottom(40);
        table.add(menuBtn).width(300).height(66).padRight(30);
        table.add(retryBtn).width(300).height(66);
        stage.addActor(table);
    }

    // ============================================================
    // RENDER
    // ============================================================
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(background, 0, 0, VW, VH);
        batch.setColor(0.55f, 0.22f, 0.32f, 0.72f);
        batch.draw(background, 0, 0, VW, VH);
        batch.setColor(1f, 1f, 1f, 1f);

        // TITRE
        String title = "TEMPS ECOULE !";
        titleFont.setColor(0f, 0f, 0f, 0.55f);
        layout.setText(titleFont, title);
        float tx = (VW - layout.width) / 2f;
        titleFont.draw(batch, title, tx + 4, TITLE_Y + 4);
        titleFont.setColor(1f, 0.55f, 0.65f, 1f);
        titleFont.draw(batch, title, tx, TITLE_Y);

        // SOUS-TITRE (avec smallFont pour être sûr)
        String sub = "Vous n'avez pas termine a temps...";
        smallFont.setColor(0.92f, 0.82f, 0.85f, 1f);
        layout.setText(smallFont, sub);
        smallFont.draw(batch, sub, (VW - layout.width) / 2f, SUB_Y);

        // PANNEAU
        batch.setColor(0f, 0f, 0f, 0.4f);
        batch.draw(panelTex, panelX + 5, panelY - 5, panelW, panelH);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(panelTex, panelX, panelY, panelW, panelH);

        renderScoresInsidePanel();

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    private void renderScoresInsidePanel() {
        float innerLeft  = panelX + 40f;
        float innerRight = panelX + panelW - 40f;
        float lineH      = 45f;
        float curY       = panelY + panelH - 25f;

        // "SCORES FINAUX"
        BitmapFont bodyFont = FontManager.getInstance().getBody();
        bodyFont.setColor(1f, 0.92f, 0.4f, 1f);
        layout.setText(bodyFont, "SCORES FINAUX");
        bodyFont.draw(batch, "SCORES FINAUX", panelX + (panelW - layout.width) / 2f, curY);
        curY -= 45f;

        // Temps écoulé
        smallFont.setColor(Color.ORANGE);
        String timeStr = "Temps ecoule : " + formatTime((int) timeElapsed);
        layout.setText(smallFont, timeStr);
        smallFont.draw(batch, timeStr, panelX + (panelW - layout.width) / 2f, curY);
        curY -= 20f;
        
        // Séparateur
        smallFont.setColor(new Color(0.4f, 0.2f, 0.3f, 0.8f));
        String sep = "─────────────────────────────────────────";
        layout.setText(smallFont, sep);
        smallFont.draw(batch, sep, panelX + (panelW - layout.width) / 2f, curY);
        curY -= 35f;

        // Podium distants
        int maxDisplay = Math.min(rankedPlayers.size(), 5);
        for (int i = 0; i < maxDisplay; i++) {
            PlayerScore ps = rankedPlayers.get(i);
            
            Color rankColor;
            String rankLabel;
            switch (i) {
                case 0: rankColor = COLOR_GOLD;   rankLabel = "1er"; break;
                case 1: rankColor = COLOR_SILVER; rankLabel = "2e";  break;
                case 2: rankColor = COLOR_BRONZE; rankLabel = "3e";  break;
                default: rankColor = COLOR_OTHER; rankLabel = (i+1) + "e"; break;
            }

            smallFont.setColor(rankColor);

            // Rang
            smallFont.draw(batch, rankLabel, innerLeft, curY);

            // Nom
            layout.setText(smallFont, ps.playerName);
            float nameX = panelX + panelW / 2f - layout.width / 2f;
            smallFont.draw(batch, ps.playerName, nameX, curY);

            // Score
            String scoreStr = ps.score + " pieces";
            layout.setText(smallFont, scoreStr);
            smallFont.draw(batch, scoreStr, innerRight - layout.width, curY);

            curY -= lineH;
        }

        // Message d'encouragement
        if (curY > panelY + 20f) {
            smallFont.setColor(Color.ORANGE);
            String enc = "Reessayez pour ameliorer votre score !";
            layout.setText(smallFont, enc);
            float encX = panelX + (panelW - layout.width) / 2f;
            smallFont.draw(batch, enc, encX, panelY + 35f);
        }
    }

    // ============================================================
    // NAVIGATION
    // ============================================================
    private void returnToMenu() {
        networkManager.setNetworkListener(null);
        if (networkManager.isConnected()) networkManager.disconnect();
        new Thread(() -> {
            try {
                Thread.sleep(250);
                Gdx.app.postRunnable(() -> game.setScreen(new TitleScreen(game)));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        titleFont = null; smallFont = null;
        if (background   != null) background.dispose();
        if (panelTex     != null) panelTex.dispose();
        if (btnMenuTex   != null) btnMenuTex.dispose();
        if (btnMenuHovTex!= null) btnMenuHovTex.dispose();
        if (btnRetryTex  != null) btnRetryTex.dispose();
    }
}
