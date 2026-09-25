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
 * LEVELWINSCREEN - Victoire ! Tous les joueurs ont terminé le niveau.
 *
 * Layout :
 *  - Titre "VICTOIRE !" en haut (hors panneau)
 *  - Panneau vert central : podium avec rang + nom + score (5 joueurs minimum)
 *  - Boutons "Retour au menu" / "Niveau suivant" EN DESSOUS du panneau
 *
 * IMPORTANT : Ne JAMAIS appeler getData().setScale() sur les polices FontManager
 * car elles sont partagées entre tous les écrans !
 */
public class LevelWinScreen implements Screen {

    // ============================================================
    // CHAMPS
    // ============================================================
    private final PlateformerGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Stage stage;
    private GlyphLayout layout;

    // Polices (non-owned — partagées par FontManager)
    private BitmapFont titleFont;
    private BitmapFont podiumFont;   // smallFont (20pt) — pour le podium
    private BitmapFont buttonFont;   // smallFont — pour les boutons

    private Texture background;
    private Texture panelTex;
    private Texture btnMenuTex, btnMenuHovTex;
    private Texture btnReplayTex;

    // Données
    private java.util.List<PlayerScore> rankedPlayers;
    private NetworkManager networkManager;

    private static final int VW = 1080;
    private static final int VH = 720;

    // Dimensions panneau podium
    private static final int PANEL_W   = 700;
    private static final int PANEL_H   = 370;  // Assez grand pour 5 joueurs
    private static final float PANEL_X = (VW - PANEL_W) / 2f;
    private static final float PANEL_Y = 140f; // Positionné au-dessus des boutons

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
            return Integer.compare(other.score, this.score); // Tri décroissant
        }
    }

    // ============================================================
    // CONSTRUCTEUR
    // ============================================================
    public LevelWinScreen(PlateformerGame game, int localPlayerScore,
                          Map<Integer, Integer> remotePlayerScores) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.layout = new GlyphLayout();

        Map<Integer, PlayerScore> playerScores = new HashMap<>();

        int localId = networkManager.getLocalPlayerId();
        playerScores.put(localId, new PlayerScore(
            localId,
            networkManager.isHost() ? "Vous (Hote)" : "Vous",
            localPlayerScore
        ));

        for (Map.Entry<Integer, Integer> e : remotePlayerScores.entrySet()) {
            playerScores.put(e.getKey(), new PlayerScore(
                e.getKey(), "Joueur " + e.getKey(), e.getValue()
            ));
        }

        rankedPlayers = new ArrayList<>(playerScores.values());
        Collections.sort(rankedPlayers);
        System.out.println("LevelWinScreen créé avec " + rankedPlayers.size() + " joueurs");
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

        // IMPORTANT: on utilise getSmall() pour le podium et les boutons
        // afin d'éviter tout appel à getData().setScale() sur une police partagée
        titleFont  = FontManager.getInstance().getTitle();
        podiumFont = FontManager.getInstance().getSmall();
        buttonFont = FontManager.getInstance().getSmall();

        background   = new Texture(Gdx.files.internal("Backgrounds/background_color_hills.png"));
        panelTex     = roundRect(PANEL_W, PANEL_H, new Color(0.45f, 0.82f, 0.38f, 0.94f));
        btnMenuTex   = roundRect(270, 65, new Color(1f, 0.71f, 0.78f, 1f));
        btnMenuHovTex= roundRect(270, 65, new Color(1f, 0.85f, 0.91f, 1f));
        btnReplayTex = roundRect(270, 65, new Color(0.72f, 0.72f, 0.76f, 1f));

        stage = new Stage(new FitViewport(VW, VH));
        Gdx.input.setInputProcessor(stage);
        createButtonUI();

        System.out.println("LevelWinScreen affiché");
    }

    private void createButtonUI() {
        // Bouton "Retour au menu" (rose)
        TextButton.TextButtonStyle menuStyle = new TextButton.TextButtonStyle();
        menuStyle.font      = buttonFont;
        menuStyle.fontColor = Color.WHITE;
        menuStyle.up   = new TextureRegionDrawable(new TextureRegion(btnMenuTex));
        menuStyle.over = new TextureRegionDrawable(new TextureRegion(btnMenuHovTex));
        menuStyle.down = new TextureRegionDrawable(new TextureRegion(btnMenuTex));

        TextButton menuBtn = new TextButton("Retour au menu", menuStyle);
        menuBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { returnToMenu(); }
        });

        // Bouton "Niveau suivant" (grisé)
        TextButton.TextButtonStyle replayStyle = new TextButton.TextButtonStyle();
        replayStyle.font      = buttonFont;
        replayStyle.fontColor = new Color(0.65f, 0.65f, 0.65f, 1f);
        replayStyle.up = new TextureRegionDrawable(new TextureRegion(btnReplayTex));

        TextButton replayBtn = new TextButton("Niveau suivant", replayStyle);
        replayBtn.setDisabled(true);

        Table table = new Table();
        table.setFillParent(true);
        table.bottom().padBottom(42);
        table.add(menuBtn).width(270).height(65).padRight(28);
        table.add(replayBtn).width(270).height(65);
        stage.addActor(table);
    }

    // ============================================================
    // RENDER
    // ============================================================
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.4f, 0.7f, 0.3f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Fond lumineux verdâtre
        batch.setColor(0.82f, 0.96f, 0.72f, 1f);
        batch.draw(background, 0, 0, VW, VH);
        batch.setColor(1f, 1f, 1f, 1f);

        // ── PANNEAU (podium seulement, hors titre et boutons) ──
        batch.setColor(0f, 0f, 0f, 0.30f);
        batch.draw(panelTex, PANEL_X + 5, PANEL_Y - 5, PANEL_W, PANEL_H);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(panelTex, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);

        // ── TITRE "VICTOIRE !" (au-dessus du panneau) ──
        String title = "VICTOIRE !";
        titleFont.setColor(0f, 0f, 0f, 0.5f);
        layout.setText(titleFont, title);
        float tx = (VW - layout.width) / 2f;
        titleFont.draw(batch, title, tx + 4, VH - 52 + 4);
        titleFont.setColor(1f, 0.97f, 0.45f, 1f);
        titleFont.draw(batch, title, tx, VH - 52);

        // ── CONTENU DU PANNEAU : podium ──
        renderPodiumInsidePanel();

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    /**
     * Affiche le classement à l'intérieur du panneau vert.
     * Utilise podiumFont (getSmall() = 20pt) — JAMAIS getData().setScale().
     */
    private void renderPodiumInsidePanel() {
        float innerLeft  = PANEL_X + 50f;
        float innerRight = PANEL_X + PANEL_W - 50f;
        float lineH      = 54f;  // Hauteur par ligne (panneau 370px / 5 lignes + header = ok)
        float headerH    = 48f;
        float curY       = PANEL_Y + PANEL_H - 22f;

        // ── En-tête "PODIUM" ──
        BitmapFont bodyFont = FontManager.getInstance().getBody();
        bodyFont.setColor(new Color(0.15f, 0.55f, 0.10f, 1f));
        layout.setText(bodyFont, "PODIUM");
        bodyFont.draw(batch, "PODIUM", PANEL_X + (PANEL_W - layout.width) / 2f, curY);
        curY -= headerH;

        // Ligne séparatrice simulée (texte tirets)
        podiumFont.setColor(new Color(0.3f, 0.65f, 0.25f, 0.8f));
        String sep = "──────────────────────────────────────────";
        layout.setText(podiumFont, sep);
        podiumFont.draw(batch, sep, PANEL_X + (PANEL_W - layout.width) / 2f, curY);
        curY -= 28f;

        // ── Lignes du classement ──
        int maxDisplay = Math.min(rankedPlayers.size(), 5); // max 5 joueurs affichés
        for (int i = 0; i < maxDisplay; i++) {
            PlayerScore ps = rankedPlayers.get(i);

            // Couleur de rang
            Color rankColor;
            String rankLabel;
            switch (i) {
                case 0: rankColor = COLOR_GOLD;   rankLabel = "1er"; break;
                case 1: rankColor = COLOR_SILVER; rankLabel = "2e";  break;
                case 2: rankColor = COLOR_BRONZE; rankLabel = "3e";  break;
                default: rankColor = COLOR_OTHER; rankLabel = (i+1) + "e"; break;
            }

            podiumFont.setColor(rankColor);

            // Rang (aligné à gauche)
            podiumFont.draw(batch, rankLabel, innerLeft, curY);

            // Nom (au centre)
            layout.setText(podiumFont, ps.playerName);
            float nameX = PANEL_X + PANEL_W / 2f - layout.width / 2f;
            podiumFont.draw(batch, ps.playerName, nameX, curY);

            // Score (aligné à droite)
            String scoreStr = ps.score + " pieces";
            layout.setText(podiumFont, scoreStr);
            podiumFont.draw(batch, scoreStr, innerRight - layout.width, curY);

            curY -= lineH;
            if (curY < PANEL_Y + 14f) break; // Sécurité
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
        titleFont = null; podiumFont = null; buttonFont = null; // Non-owned
        if (background   != null) background.dispose();
        if (panelTex     != null) panelTex.dispose();
        if (btnMenuTex   != null) btnMenuTex.dispose();
        if (btnMenuHovTex!= null) btnMenuHovTex.dispose();
        if (btnReplayTex != null) btnReplayTex.dispose();
        System.out.println("LevelWinScreen nettoyé");
    }
}
