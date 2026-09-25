package com.vortexmakers.plateformer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * FONTMANAGER - Singleton gérant les polices FreeType.
 * Chargez une fois, partagez entre tous les écrans.
 * La police FredokaOne-Regular.ttf doit être dans assets/Fonts/
 */
public class FontManager {

    private static FontManager instance;

    private BitmapFont fontTitle;      // Grande police pour titres (ex: 90pt)
    private BitmapFont fontSubtitle;   // Police sous-titre (ex: 36pt)
    private BitmapFont fontButton;     // Police boutons (ex: 32pt)
    private BitmapFont fontBody;       // Police corps de texte (ex: 24pt)
    private BitmapFont fontSmall;      // Petite police (ex: 18pt)
    private BitmapFont fontHUD;        // Police HUD en jeu (ex: 28pt)

    private boolean loaded = false;

    private static final String FONT_PATH = "Fonts/FredokaOne-Regular.ttf";
    private static final String FALLBACK_PATH = "Fonts/fallback.ttf"; // non utilisé pour l'instant

    // Caractères à inclure dans le bitmap généré (ASCII + accents français)
    private static final String CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
        "!?.,;:+-*/=()[]{}\"'@#%&_<>|\\~^`" +
        " àâäéèêëîïôùûüçÀÂÄÉÈÊËÎÏÔÙÛÜÇœŒæÆ" +
        "🎮⚙✕🏆⏱";

    private FontManager() {}

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    /**
     * Charge toutes les polices. Appeler une fois au démarrage (ex: dans TitleScreen.show())
     * ou dans PlateformerGame.create().
     */
    public void load() {
        if (loaded) return;

        if (!Gdx.files.internal(FONT_PATH).exists()) {
            System.out.println("[FontManager] ATTENTION: " + FONT_PATH + " introuvable ! Utilisation de la police par défaut.");
            loadFallbackFonts();
            loaded = true;
            return;
        }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));

        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.characters = CHARS;

        // Titre (très grande, avec bordure)
        param.size = 90;
        param.borderWidth = 3.5f;
        param.borderColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.85f);
        param.shadowOffsetX = 3;
        param.shadowOffsetY = -3;
        param.shadowColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.4f);
        fontTitle = generator.generateFont(param);

        // Sous-titre
        param = new FreeTypeFontParameter();
        param.characters = CHARS;
        param.size = 36;
        param.borderWidth = 1.5f;
        param.borderColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.5f);
        fontSubtitle = generator.generateFont(param);

        // Boutons
        param = new FreeTypeFontParameter();
        param.characters = CHARS;
        param.size = 34;
        param.borderWidth = 1.5f;
        param.borderColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.6f);
        fontButton = generator.generateFont(param);

        // Corps
        param = new FreeTypeFontParameter();
        param.characters = CHARS;
        param.size = 26;
        param.borderWidth = 1f;
        param.borderColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.4f);
        fontBody = generator.generateFont(param);

        // Petit
        param = new FreeTypeFontParameter();
        param.characters = CHARS;
        param.size = 20;
        fontSmall = generator.generateFont(param);

        // HUD
        param = new FreeTypeFontParameter();
        param.characters = CHARS;
        param.size = 30;
        param.borderWidth = 2f;
        param.borderColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.7f);
        fontHUD = generator.generateFont(param);

        generator.dispose();
        loaded = true;
        System.out.println("[FontManager] Polices FreeType chargées avec succès.");
    }

    /**
     * Fallback si la police TTF n'est pas disponible.
     * Utilise la BitmapFont par défaut de LibGDX à une taille correcte.
     */
    private void loadFallbackFonts() {
        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(4.0f);

        fontSubtitle = new BitmapFont();
        fontSubtitle.getData().setScale(1.8f);

        fontButton = new BitmapFont();
        fontButton.getData().setScale(1.8f);

        fontBody = new BitmapFont();
        fontBody.getData().setScale(1.4f);

        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(1.0f);

        fontHUD = new BitmapFont();
        fontHUD.getData().setScale(1.6f);

        System.out.println("[FontManager] Polices de fallback chargées.");
    }

    public boolean isLoaded() { return loaded; }

    public BitmapFont getTitle()    { return fontTitle; }
    public BitmapFont getSubtitle() { return fontSubtitle; }
    public BitmapFont getButton()   { return fontButton; }
    public BitmapFont getBody()     { return fontBody; }
    public BitmapFont getSmall()    { return fontSmall; }
    public BitmapFont getHUD()      { return fontHUD; }

    /**
     * À appeler UNIQUEMENT depuis PlateformerGame.dispose() ou lors d'un reload complet.
     */
    public void dispose() {
        if (fontTitle != null)    { fontTitle.dispose();    fontTitle = null; }
        if (fontSubtitle != null) { fontSubtitle.dispose(); fontSubtitle = null; }
        if (fontButton != null)   { fontButton.dispose();   fontButton = null; }
        if (fontBody != null)     { fontBody.dispose();     fontBody = null; }
        if (fontSmall != null)    { fontSmall.dispose();    fontSmall = null; }
        if (fontHUD != null)      { fontHUD.dispose();      fontHUD = null; }
        loaded = false;
        instance = null;
        System.out.println("[FontManager] Polices libérées.");
    }
}
