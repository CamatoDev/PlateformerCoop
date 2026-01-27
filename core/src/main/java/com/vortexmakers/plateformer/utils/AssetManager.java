package com.vortexmakers.plateformer.utils;


// IMPORTATIONS ===========================================
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * ASSETMANAGER - Gère le chargement et l'accès à toutes les ressources graphiques
 *
 * Responsabilités :
 * - Charger les textures au démarrage
 * - Fournir un accès centralisé aux assets
 * - Gérer la disposition des ressources
 * - Optimiser la mémoire en évitant les doublons
 *
 * Design Pattern : Singleton pour un accès global
 */
public class AssetManager implements Disposable {

    // INSTANCE SINGLETON
    private static AssetManager instance;

    // TEXTURES - PLAYER
    private Texture playerIdle;
    private Texture playerJump;
    private Texture playerWalkA;
    private Texture playerWalkB;
    private Texture playerHit;

    // TEXTURES - PLATFORMES
    private Texture platformBlock;
    private Texture platformTerrain;

    // TEXTURES - COLLECTIBLES
    private Texture coinTexture;
    private Texture keyTexture;

    // ✅ NOUVEAU : TEXTURES - DRAPEAU
    private Texture flagGreenA;
    private Texture flagGreenB;

    // TEXTURES - BACKGROUND
    private Texture backgroundSky;
    private Texture backgroundHills;
    private Texture backgroundClouds;
    private Texture backgroundTrees;

    // ÉTATS
    private boolean assetsLoaded = false;

    /**
     * CONSTRUCTEUR PRIVÉ - Pattern Singleton
     *
     * Pourquoi Singleton ?
     * → Évite d'avoir plusieurs instances qui chargent les mêmes assets
     * → Accès global depuis n'importe quelle classe
     * → Contrôle centralisé du cycle de vie
     */
    private AssetManager() {
        // Le chargement sera fait explicitement via loadAssets()
    }

    /**
     * POINT D'ACCÈS GLOBAL - Retourne l'instance unique
     */
    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    /**
     * CHARGEMENT DE TOUS LES ASSETS
     *
     * Pourquoi une méthode séparée pour le chargement ?
     * → Permet de contrôler quand le chargement a lieu
     * → Peut afficher un écran de chargement
     * → Gestion d'erreurs centralisée
     */
    public void loadAssets() {
        try {
            // PLAYER - Characters (128x128)
            playerIdle = new Texture(Gdx.files.internal("Characters/character_beige_idle.png"));
            playerJump = new Texture(Gdx.files.internal("Characters/character_beige_jump.png"));
            playerWalkA = new Texture(Gdx.files.internal("Characters/character_beige_walk_a.png"));
            playerWalkB = new Texture(Gdx.files.internal("Characters/character_beige_walk_b.png"));
            playerHit = new Texture(Gdx.files.internal("Characters/character_beige_hit.png"));

            // PLATFORMES - Tiles (64x64)
            platformBlock = new Texture(Gdx.files.internal("Tiles/block_empty.png"));
            platformTerrain = new Texture(Gdx.files.internal("Tiles/terrain_grass_block_top.png"));

            // DRAPEAU - Tiles (128x128)
            flagGreenA = new Texture(Gdx.files.internal("Tiles/Double/flag_red_a.png"));
            flagGreenB = new Texture(Gdx.files.internal("Tiles/Double/flag_red_b.png"));

            // COLLECTIBLES - Tiles (64x64)
            coinTexture = new Texture(Gdx.files.internal("Tiles/coin_gold.png"));
            keyTexture = new Texture(Gdx.files.internal("Tiles/key_yellow.png"));

            // BACKGROUND - Backgrounds (256x256)
            backgroundSky = new Texture(Gdx.files.internal("Backgrounds/background_solid_sky.png"));
            backgroundHills = new Texture(Gdx.files.internal("Backgrounds/background_color_hills.png"));
            backgroundClouds = new Texture(Gdx.files.internal("Backgrounds/background_clouds.png"));
            backgroundTrees = new Texture(Gdx.files.internal("Backgrounds/background_color_trees.png"));

            // CONFIGURATION DES TEXTURES
            // Pour le pixel art, on veut un filtrage "nearest" pour rester net
            setTextureFilterNearest();

            assetsLoaded = true;
            System.out.println("Assets chargés avec succès !");

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des assets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * CONFIGURATION DU FILTRE TEXTURE - Néarest pour pixel art
     *
     * Pourquoi Nearest et pas Linear ?
     * → Pixel art : on veut des pixels nets, pas flous
     * → Linear ferait de l'anti-aliasing et flouterait l'image
     * → Nearest préserve les contours nets
     */
    private void setTextureFilterNearest() {
        playerIdle.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerJump.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerWalkA.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerWalkB.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerHit.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        platformBlock.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        platformTerrain.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        coinTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        keyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Drapeau
        flagGreenA.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        flagGreenB.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        backgroundSky.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backgroundHills.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backgroundClouds.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backgroundTrees.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    // GETTERS - PLAYER

    public Texture getPlayerIdle() {
        checkAssetsLoaded();
        return playerIdle;
    }

    public Texture getPlayerJump() {
        checkAssetsLoaded();
        return playerJump;
    }

    public Texture getPlayerWalkA() {
        checkAssetsLoaded();
        return playerWalkA;
    }

    public Texture getPlayerWalkB() {
        checkAssetsLoaded();
        return playerWalkB;
    }

    public Texture getPlayerHit() {
        checkAssetsLoaded();
        return playerHit;
    }

    // GETTERS - PLATFORMES

    public Texture getPlatformBlock() {
        checkAssetsLoaded();
        return platformBlock;
    }

    public Texture getPlatformTerrain() {
        checkAssetsLoaded();
        return platformTerrain;
    }

    // GETTERS - COLLECTIBLES

    public Texture getCoinTexture() {
        checkAssetsLoaded();
        return coinTexture;
    }

    public Texture getKeyTexture() {
        checkAssetsLoaded();
        return keyTexture;
    }

    // Getters drapeau
    public Texture getFlagGreenA() {
        checkAssetsLoaded();
        return flagGreenA;
    }

    public Texture getFlagGreenB() {
        checkAssetsLoaded();
        return flagGreenB;
    }

    // GETTERS - BACKGROUND

    public Texture getBackgroundSky() {
        checkAssetsLoaded();
        return backgroundSky;
    }

    public Texture getBackgroundHills() {
        checkAssetsLoaded();
        return backgroundHills;
    }

    public Texture getBackgroundClouds() {
        checkAssetsLoaded();
        return backgroundClouds;
    }

    public Texture getBackgroundTrees() {
        checkAssetsLoaded();
        return backgroundTrees;
    }

    /**
     * VÉRIFICATION QUE LES ASSETS SONT CHARGÉS
     *
     * Sécurité : évite les NullPointerException
     */
    private void checkAssetsLoaded() {
        if (!assetsLoaded) {
            throw new IllegalStateException("Assets non chargés ! Appeler loadAssets() d'abord.");
        }
    }

    /**
     * LIBÉRATION DES RESSOURCES - Très important !
     */
    @Override
    public void dispose() {
        // PLAYER
        if (playerIdle != null) playerIdle.dispose();
        if (playerJump != null) playerJump.dispose();
        if (playerWalkA != null) playerWalkA.dispose();
        if (playerWalkB != null) playerWalkB.dispose();
        if (playerHit != null) playerHit.dispose();

        // PLATFORMES
        if (platformBlock != null) platformBlock.dispose();
        if (platformTerrain != null) platformTerrain.dispose();

        // COLLECTIBLES
        if (coinTexture != null) coinTexture.dispose();
        if (keyTexture != null) keyTexture.dispose();

        // Drapeau
        if (flagGreenA != null) flagGreenA.dispose();
        if (flagGreenB != null) flagGreenB.dispose();

        // BACKGROUND
        if (backgroundSky != null) backgroundSky.dispose();
        if (backgroundHills != null) backgroundHills.dispose();
        if (backgroundClouds != null) backgroundClouds.dispose();
        if (backgroundTrees != null) backgroundTrees.dispose();

        assetsLoaded = false;
        System.out.println("Assets libérés de la mémoire");
    }

    /**
     * STATUT DE CHARGEMENT
     */
    public boolean areAssetsLoaded() {
        return assetsLoaded;
    }
}
