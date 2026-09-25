package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.vortexmakers.plateformer.utils.FontManager;

/**
 * TITLESCREEN - Écran d'accueil du jeu Kawaii Verse.
 * Affiche le titre multicolore, les boutons Jouer/Paramètres/Quitter.
 * Utilise FontManager pour des polices nettes (FreeType si disponible).
 */
public class TitleScreen implements Screen {

    private final PlateformerGame game;
    private SpriteBatch batch;
    private Stage stage;
    private GlyphLayout layout;

    // Textures
    private Texture background;
    private Texture btnPlayTex, btnPlayHoverTex, btnPlayDownTex;
    private Texture btnSettingsTex, btnSettingsHoverTex;
    private Texture btnQuitTex, btnQuitHoverTex;

    // Couleurs des lettres du titre (style référence : alternance rose/bleu/jaune)
    private static final Color[] TITLE_COLORS = {
        new Color(1f, 0.55f, 0.7f, 1f),   // K - rose
        new Color(0.55f, 0.8f, 1f, 1f),   // A - bleu
        new Color(1f, 0.87f, 0.35f, 1f),  // W - jaune
        new Color(1f, 0.55f, 0.7f, 1f),   // A - rose
        new Color(0.55f, 0.8f, 1f, 1f),   // I - bleu
        new Color(1f, 0.87f, 0.35f, 1f),  // I - jaune
        new Color(1f, 1f, 1f, 1f),        // espace
        new Color(1f, 0.55f, 0.7f, 1f),   // V - rose
        new Color(0.55f, 0.8f, 1f, 1f),   // E - bleu
        new Color(1f, 0.87f, 0.35f, 1f),  // R - jaune
        new Color(1f, 0.55f, 0.7f, 1f),   // S - rose
        new Color(0.55f, 0.8f, 1f, 1f),   // E - bleu
    };

    private static final int VW = 1080;
    private static final int VH = 720;

    public TitleScreen(PlateformerGame game) {
        this.game = game;
    }

    private Texture createRoundedRect(int w, int h, Color color) {
        Pixmap px = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        px.setColor(color);
        int r = Math.min(h / 2, 28);
        px.fillRectangle(r, 0, w - 2 * r, h);
        px.fillRectangle(0, r, w, h - 2 * r);
        px.fillCircle(r, r, r);
        px.fillCircle(w - r, r, r);
        px.fillCircle(r, h - r, r);
        px.fillCircle(w - r, h - r, r);
        Texture t = new Texture(px);
        px.dispose();
        return t;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new FitViewport(VW, VH));
        Gdx.input.setInputProcessor(stage);
        layout = new GlyphLayout();

        // S'assurer que les polices sont chargées
        FontManager.getInstance().load();

        background = new Texture(Gdx.files.internal("Backgrounds/background_color_hills.png"));

        // Textures boutons avec hover
        btnPlayTex        = createRoundedRect(360, 78, new Color(1f, 0.71f, 0.78f, 1f));
        btnPlayHoverTex   = createRoundedRect(360, 78, new Color(1f, 0.85f, 0.90f, 1f));
        btnPlayDownTex    = createRoundedRect(360, 78, new Color(0.9f, 0.60f, 0.68f, 1f));

        btnSettingsTex      = createRoundedRect(360, 78, new Color(0.72f, 0.89f, 1f, 1f));
        btnSettingsHoverTex = createRoundedRect(360, 78, new Color(0.85f, 0.95f, 1f, 1f));

        btnQuitTex      = createRoundedRect(360, 78, new Color(0.79f, 0.72f, 1f, 1f));
        btnQuitHoverTex = createRoundedRect(360, 78, new Color(0.90f, 0.85f, 1f, 1f));

        BitmapFont btnFont = FontManager.getInstance().getButton();

        // Style bouton Jouer (rose)
        TextButton.TextButtonStyle playStyle = new TextButton.TextButtonStyle();
        playStyle.font      = btnFont;
        playStyle.fontColor = Color.WHITE;
        playStyle.up   = new TextureRegionDrawable(new TextureRegion(btnPlayTex));
        playStyle.over = new TextureRegionDrawable(new TextureRegion(btnPlayHoverTex));
        playStyle.down = new TextureRegionDrawable(new TextureRegion(btnPlayDownTex));

        // Style bouton Paramètres (bleu)
        TextButton.TextButtonStyle settingsStyle = new TextButton.TextButtonStyle();
        settingsStyle.font      = btnFont;
        settingsStyle.fontColor = Color.WHITE;
        settingsStyle.up   = new TextureRegionDrawable(new TextureRegion(btnSettingsTex));
        settingsStyle.over = new TextureRegionDrawable(new TextureRegion(btnSettingsHoverTex));
        settingsStyle.down = new TextureRegionDrawable(new TextureRegion(btnSettingsTex));

        // Style bouton Quitter (violet)
        TextButton.TextButtonStyle quitStyle = new TextButton.TextButtonStyle();
        quitStyle.font      = btnFont;
        quitStyle.fontColor = Color.WHITE;
        quitStyle.up   = new TextureRegionDrawable(new TextureRegion(btnQuitTex));
        quitStyle.over = new TextureRegionDrawable(new TextureRegion(btnQuitHoverTex));
        quitStyle.down = new TextureRegionDrawable(new TextureRegion(btnQuitTex));

        TextButton playBtn = new TextButton("Jouer", playStyle);
        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                game.setScreen(new LobbyScreen(game));
            }
        });

        TextButton settingsBtn = new TextButton("Parametres", settingsStyle);
        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                game.setScreen(new SettingsScreen(game));
            }
        });

        TextButton quitBtn = new TextButton("Quitter", quitStyle);
        quitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Table centrée — boutons dans la moitié inférieure de l'écran
        Table table = new Table();
        table.setFillParent(true);
        table.bottom().padBottom(130);
        table.add(playBtn).width(360).height(78).padBottom(20);
        table.row();
        table.add(settingsBtn).width(360).height(78).padBottom(20);
        table.row();
        table.add(quitBtn).width(360).height(78);
        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.5f, 0.7f, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // --- Fond plein écran ---
        batch.draw(background, 0, 0, VW, VH);

        BitmapFont titleFont    = FontManager.getInstance().getTitle();
        BitmapFont subtitleFont = FontManager.getInstance().getSubtitle();
        BitmapFont smallFont    = FontManager.getInstance().getSmall();

        // --- Titre KAWAII VERSE lettre par lettre multicolore ---
        drawMulticolorTitle(titleFont, "KAWAII VERSE", VH - 95);

        // --- Sous-titre ---
        String sub = "The Ultimate Cute Race !";
        subtitleFont.setColor(new Color(0.25f, 0.20f, 0.35f, 1f));
        layout.setText(subtitleFont, sub);
        subtitleFont.draw(batch, sub, (VW - layout.width) / 2f, VH - 175);

        // --- Version ---
        smallFont.setColor(0.8f, 0.8f, 0.8f, 0.9f);
        smallFont.draw(batch, "v1.0", VW - 70, 35);

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    /**
     * Dessine chaque lettre du titre dans une couleur alternée (style référence kawaii).
     * Chaque lettre a un contour noir épais grâce à FreeType (déjà dans la font).
     */
    private void drawMulticolorTitle(BitmapFont font, String text, float y) {
        // Mesurer la largeur totale pour centrer
        font.setColor(Color.WHITE);
        layout.setText(font, text);
        float totalWidth = layout.width;
        float startX = (VW - totalWidth) / 2f;

        float curX = startX;
        int colorIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            String letter = String.valueOf(text.charAt(i));

            if (letter.equals(" ")) {
                // Mesurer un espace
                layout.setText(font, " ");
                curX += layout.width;
                colorIndex++;
                continue;
            }

            // Couleur de cette lettre
            Color c = TITLE_COLORS[Math.min(colorIndex, TITLE_COLORS.length - 1)];
            font.setColor(c);
            layout.setText(font, letter);
            font.draw(batch, letter, curX, y);

            curX += layout.width;
            colorIndex++;
        }
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        if (background != null) background.dispose();
        if (btnPlayTex != null) btnPlayTex.dispose();
        if (btnPlayHoverTex != null) btnPlayHoverTex.dispose();
        if (btnPlayDownTex != null) btnPlayDownTex.dispose();
        if (btnSettingsTex != null) btnSettingsTex.dispose();
        if (btnSettingsHoverTex != null) btnSettingsHoverTex.dispose();
        if (btnQuitTex != null) btnQuitTex.dispose();
        if (btnQuitHoverTex != null) btnQuitHoverTex.dispose();
        // Les polices sont gérées par FontManager, ne pas les dispose ici
    }
}
