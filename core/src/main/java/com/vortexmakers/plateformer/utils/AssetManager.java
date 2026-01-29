package com.vortexmakers.plateformer.utils;


// IMPORTATIONS ===========================================
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

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

    // TEXTURES - PLAYERS (tous les personnages)
    private Map<String, PlayerTextures> characterTextures;

    // TEXTURES - PLATFORMES
    private Texture platformBlock;
    private Texture platformTerrain;

    // TEXTURES - COLLECTIBLES
    private Texture coinTexture;
    private Texture keyTexture;

    // TEXTURES - PIÈGES
    private Texture spikeTexture;

    // TEXTURES - DRAPEAU
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
            // PLAYERS - Tous les personnages (128x128)
            characterTextures = new HashMap<>();
            characterTextures.put("beige", loadCharacterTextures("character_beige"));
            characterTextures.put("green", loadCharacterTextures("character_green"));
            characterTextures.put("pink", loadCharacterTextures("character_pink"));
            characterTextures.put("purple", loadCharacterTextures("character_purple"));
            characterTextures.put("yellow", loadCharacterTextures("character_yellow"));

            // PLATFORMES - Tiles (64x64)
            platformBlock = new Texture(Gdx.files.internal("Tiles/block_empty.png"));
            platformTerrain = new Texture(Gdx.files.internal("Tiles/terrain_grass_block_top.png"));

            // DRAPEAU - Tiles (128x128)
            flagGreenA = new Texture(Gdx.files.internal("Tiles/Double/flag_red_a.png"));
            flagGreenB = new Texture(Gdx.files.internal("Tiles/Double/flag_red_b.png"));

            // COLLECTIBLES - Tiles (64x64)
            coinTexture = new Texture(Gdx.files.internal("Tiles/coin_gold.png"));
            keyTexture = new Texture(Gdx.files.internal("Tiles/key_yellow.png"));

            // PIÈGES - Tiles (64x64)
            spikeTexture = new Texture(Gdx.files.internal("Tiles/spikes.png"));

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
     * CHARGER TOUTES LES TEXTURES D'UN PERSONNAGE
     */
    private PlayerTextures loadCharacterTextures(String characterName) {
        Texture idle = new Texture(Gdx.files.internal("Characters/" + characterName + "_idle.png"));
        Texture jump = new Texture(Gdx.files.internal("Characters/" + characterName + "_jump.png"));
        Texture walkA = new Texture(Gdx.files.internal("Characters/" + characterName + "_walk_a.png"));
        Texture walkB = new Texture(Gdx.files.internal("Characters/" + characterName + "_walk_b.png"));
        Texture hit = new Texture(Gdx.files.internal("Characters/" + characterName + "_hit.png"));

        // Configuration du filtre
        idle.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        jump.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        walkA.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        walkB.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        hit.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        System.out.println("Personnage chargé : " + characterName);

        return new PlayerTextures(idle, jump, walkA, walkB, hit);
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
        platformBlock.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        platformTerrain.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        coinTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        keyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        spikeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Drapeau
        flagGreenA.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        flagGreenB.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        backgroundSky.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backgroundHills.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backgroundClouds.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backgroundTrees.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    // GETTERS - PLAYERS

    /**
     * Obtenir les textures d'un type de personnage
     */
    public PlayerTextures getCharacterTextures(String characterType) {
        checkAssetsLoaded();
        PlayerTextures textures = characterTextures.get(characterType);
        if (textures == null) {
            System.err.println("Personnage inconnu : " + characterType + ", utilisation de beige par défaut");
            return characterTextures.get("beige");
        }
        return textures;
    }

    /**
     * Vérifier si un type de personnage existe
     */
    public boolean hasCharacter(String characterType) {
        return characterTextures.containsKey(characterType);
    }

    /**
     * Obtenir tous les types de personnages disponibles
     */
    public String[] getAvailableCharacters() {
        return new String[]{"beige", "green", "pink", "purple", "yellow"};
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

    // GETTER - PIÈGES

    public Texture getSpikeTexture() {
        checkAssetsLoaded();
        return spikeTexture;
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
        // PLAYERS
        if (characterTextures != null) {
            for (PlayerTextures textures : characterTextures.values()) {
                if (textures != null) {
                    textures.dispose();
                }
            }
            characterTextures.clear();
        }

        // PLATFORMES
        if (platformBlock != null) platformBlock.dispose();
        if (platformTerrain != null) platformTerrain.dispose();

        // COLLECTIBLES
        if (coinTexture != null) coinTexture.dispose();
        if (keyTexture != null) keyTexture.dispose();

        // PIÈGES
        if (spikeTexture != null) spikeTexture.dispose();

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
