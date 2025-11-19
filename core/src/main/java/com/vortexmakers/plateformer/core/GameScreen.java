package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.vortexmakers.plateformer.entities.Collectible;
import com.vortexmakers.plateformer.entities.Player;
import com.vortexmakers.plateformer.entities.Platform;
import com.vortexmakers.plateformer.systems.PhysicsSystem;
import com.vortexmakers.plateformer.utils.Constants;
import com.vortexmakers.plateformer.utils.AssetManager;

public class GameScreen implements Screen {
    private final PlateformerGame game;
    private OrthographicCamera gameCamera;  // Caméra pour la logique de jeu
    private OrthographicCamera uiCamera;    // Caméra pour l'interface
    private SpriteBatch batch;

    private Player player;
    // Score du joueur
    private int playerScore;

    private Array<Platform> platforms;
    private Array<Collectible> collectibles;

    private PhysicsSystem physicsSystem;

    // Échelle d'affichage
    private float scaleX, scaleY;

    // Référence à AssetManager
    private AssetManager assets;

    // Textures de background
    private Texture backgroundSky;
    private Texture backgroundHills;
    private Texture backgroundTrees;
    private Texture backgroundClouds;

    // Positions pour effet parallaxe
    private float cloudOffset = 0;
    private float treesOffset = 0f;

    public GameScreen(PlateformerGame game) {
        this.game = game;

        // INITIALISATION ASSETMANAGER
        this.assets = AssetManager.getInstance();
        assets.loadAssets(); // CHARGEMENT DES ASSETS (IMPORTANT)



        // Initialiser les caméras
        gameCamera = new OrthographicCamera();
        gameCamera.setToOrtho(false, Constants.GAME_WIDTH, Constants.GAME_HEIGHT);
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);

        batch = new SpriteBatch();

        // Récuperation de textures du background
        this.backgroundSky = assets.getBackgroundSky();
        this.backgroundHills = assets.getBackgroundHills();
        this.backgroundTrees = assets.getBackgroundTrees();
        this.backgroundClouds = assets.getBackgroundClouds();

        // Création des entités
        player = new Player(50, 300);
        platforms = new Array<>();
        collectibles = new Array<>();

        playerScore = 0;

         // Création des éléments du niveau
        createTestLevel();
        createCollectibles();
        physicsSystem = new PhysicsSystem();
    }

    private void createTestLevel() {
        // Plateforme de base (sol)
        for (int i = 0; i < 15; i++) {
            platforms.add(new Platform(i * 200, 0, 200)); // ou width: 150
        }

        // Quelques plateformes de test
        platforms.add(new Platform(200, 80, 100));
        platforms.add(new Platform(580, 150, 100));
        platforms.add(new Platform(100, 160, 85));
        platforms.add(new Platform(720, 80, 120));
        platforms.add(new Platform(500, Constants.PLATFORM_HEIGHT, 32, 80));

        // Nouvelle plateforme loin à droite
        platforms.add(new Platform(1150, 80, 400));
        platforms.add(new Platform(1400, 160, 100));
        platforms.add(new Platform(1700, 160, 150));

        // Ajoutons encore plus de plateformes pour vraiment voir le défilement
        platforms.add(new Platform(2000, 200, 200));
        platforms.add(new Platform(2300, 100, 150));
        platforms.add(new Platform(2600, 150, 120));
        platforms.add(new Platform(2900, 80, 200));
    }

    private void createCollectibles() {
        // COLLECTIBLES SUR LES PLATEFORMES PRINCIPALES

        // Plateformes basses (faciles)
        collectibles.add(new Collectible(250, 120));   // Sur plateforme à 200,80
        collectibles.add(new Collectible(600, 180));   // Sur plateforme à 550,140
        collectibles.add(new Collectible(780, 100));   // Sur plateforme à 720,80

        // Plateformes hautes (plus difficiles)
        collectibles.add(new Collectible(1200, 220));  // Sur plateforme à 1150,200
        collectibles.add(new Collectible(1700, 220));  // Sur plateforme à 1700,160
        collectibles.add(new Collectible(2050, 250));  // Sur plateforme à 2100,200

        // Collectibles nécessitant des sauts précis
        collectibles.add(new Collectible(400, 250));   // Haut de la plateforme à 400,150
        collectibles.add(new Collectible(1400, 210));  // Haut de la plateforme à 1400,160

        System.out.println("Création de " + collectibles.size + " collectibles");
    }

    @Override
    public void render(float delta) {
        updateBackground(delta);

        // Mise à jour
        update(delta);

        // Mise à jour de la caméra
        updateCamera();

        // Rendu
        batch.setProjectionMatrix(gameCamera.combined);
        batch.begin();

        // DESSIN DU BACKGROUND
        renderBackground();

        // Dessiner les plateformes
        for (Platform platform : platforms) {
            platform.render(batch);
        }

        // Dessiner les COLLECTIBLES
        for (Collectible collectible : collectibles) {
            collectible.render(batch);
        }

        // Dessiner le joueur
        player.render(batch);

        batch.end();

        // Mettre à jour la caméra
        updateCamera();
        // Mise à jour des collectibles
        updateCollectibles();
    }

    private void updateBackground(float delta) {
        // ANIMATION DES NUAGES (défilement moyen)
        cloudOffset += Constants.CLOUD_SCROLL_SPEED * delta;

        // RÉINITIALISER LES OFFSETS POUR ÉVITER LES OVERFLOWS
        if (cloudOffset > 256f * Constants.CLOUD_SCALE) {
            cloudOffset = 0;
        }
        if (treesOffset > 256f) {
            treesOffset = 0;
        }
    }

    private void update(float delta) {
        player.update(delta);
        // Pour les collisions avec les platform
        physicsSystem.checkCollisions(player, platforms);
        // Pour les collisions avec les collectibles
        int collectedThisFrame = physicsSystem.checkCollectibleCollisions(player, collectibles);
        if (collectedThisFrame > 0) {
            playerScore += collectedThisFrame;
            System.out.println("Score: " + playerScore + " (+" + collectedThisFrame + ")");
        }
    }

    private void updateCamera() {
        // SUIVI SIMPLE DU JOUEUR
        float targetX = player.getPosition().x + Constants.CAMERA_LEAD;

        // Limites de la caméra
        float minCameraX = Constants.GAME_WIDTH / 2;
        float maxCameraX = Constants.WORLD_WIDTH - Constants.GAME_WIDTH / 2;

        targetX = Math.max(minCameraX, Math.min(targetX, maxCameraX));

        // Application directe
        gameCamera.position.x = targetX;
        gameCamera.position.y = Constants.GAME_HEIGHT / 2;
        gameCamera.update();
    }

    private void renderBackground() {
        float camX = gameCamera.position.x;
        float camY = gameCamera.position.y;
        float viewportWidth = Constants.GAME_WIDTH;
        float viewportHeight = Constants.GAME_HEIGHT;

        // CALCUL DES POSITIONS =============================
        float screenLeft = camX - viewportWidth / 2;
        float screenBottom = camY - viewportHeight / 2;
        float screenRight = camX + viewportWidth / 2;
        float screenTop = camY + viewportHeight / 2;

        // 1. RENDU DU CIEL - PLEIN ÉCRAN, FIXE ============
        renderSky(screenLeft, screenBottom, screenRight, screenTop);

        // 2. RENDU DES ARBRES - BAS, RÉPÉTITION, DÉFILEMENT LENT
        renderTrees(screenLeft, screenBottom, screenRight, screenTop);

        // 3. RENDU DES NUAGES - MILIEU/HAUT, PETITS, DÉFILEMENT MOYEN
        renderClouds(screenLeft, screenBottom, screenRight, screenTop);
    }

    private void renderSky(float screenLeft, float screenBottom, float screenRight, float screenTop) {
        // TAILLE D'UNE TUILE DE CIEL (256 pixels)
        float skyTileSize = 256f;

        // CALCULER LE NOMBRE DE TUILES NÉCESSAIRES
        int tilesX = (int) Math.ceil((screenRight - screenLeft) / skyTileSize) + 1;
        int tilesY = (int) Math.ceil((screenTop - screenBottom) / skyTileSize) + 1;

        // POSITION DE DÉPART (alignée sur la grille)
        float startX = (float) Math.floor(screenLeft / skyTileSize) * skyTileSize;
        float startY = (float) Math.floor(screenBottom / skyTileSize) * skyTileSize;

        // DESSINER TOUTES LES TUILES DE CIEL
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                float tileX = startX + (x * skyTileSize);
                float tileY = startY + (y * skyTileSize);

                batch.draw(
                    backgroundSky,
                    tileX, tileY,
                    skyTileSize, skyTileSize
                );
            }
        }
    }

    private void renderTrees(float screenLeft, float screenBottom, float screenRight, float screenTop) {
        // TAILLE D'UNE TUILE D'ARBRES (256 pixels)
        float treeTileSize = 256f;

        // POSITION VERTICALE : 5% du bas de l'écran
        float treesY = screenBottom + (screenTop - screenBottom) * Constants.TREES_Y_POSITION;

        // LARGEUR VISIBLE + MARGE POUR DÉFILEMENT
        float visibleWidth = screenRight - screenLeft;

        // CALCULER LA POSITION SANS DÉFILEMENT PARALLAXE
        //float parallaxTreesX = screenLeft * 0.3f; // Défilement lent (30% de la caméra)
        float adjustedTreesX = treesOffset; // + parallaxTreesX si on veut un défilement

        // NOMBRE DE TUILES NÉCESSAIRES
        int tilesNeeded = (int) Math.ceil(visibleWidth / treeTileSize) + 2;

        // POSITION DE DÉPART (alignée sur la grille avec défilement)
        float startX = (float) Math.floor((screenLeft + adjustedTreesX) / treeTileSize) * treeTileSize - adjustedTreesX;

        // DESSINER LES TUILES D'ARBRES
        for (int i = 0; i < tilesNeeded; i++) {
            float tileX = startX + (i * treeTileSize);

            // Ne dessiner que si dans les limites du monde
            if (tileX >= 0 && tileX < Constants.WORLD_WIDTH) {
                batch.draw(
                    backgroundTrees,
                    tileX, treesY,
                    treeTileSize, treeTileSize
                );
            }
        }
    }

    private void renderClouds(float screenLeft, float screenBottom, float screenRight, float screenTop) {
        // TAILLE ORIGINALE DES NUAGES (256 pixels)
        float cloudOriginalSize = 256f;

        // ÉCHELLE RÉDUITE POUR LES NUAGES
        float cloudScaledSize = cloudOriginalSize * Constants.CLOUD_SCALE;

        // POSITION VERTICALE : 60% du haut de l'écran
        float cloudsY = screenBottom + (screenTop - screenBottom) * Constants.CLOUD_Y_POSITION;

        // LARGEUR VISIBLE
        float visibleWidth = screenRight - screenLeft;

        // CALCULER LA POSITION SANS DÉFILEMENT PARALLAXE + ANIMATION
        //float parallaxCloudsX = screenLeft * 0.5f; // Défilement moyen (50% de la caméra)
        float adjustedCloudsX = cloudOffset; // + parallaxCloudsX si on veut un défilement

        // NOMBRE DE TUILES NÉCESSAIRES
        int tilesNeeded = (int) Math.ceil(visibleWidth / cloudScaledSize) + 2;

        // POSITION DE DÉPART
        float startX = (float) Math.floor((screenLeft + adjustedCloudsX) / cloudScaledSize) * cloudScaledSize - adjustedCloudsX;

        // DESSINER LES TUILES DE NUAGES
        for (int i = 0; i < tilesNeeded; i++) {
            float tileX = startX + (i * cloudScaledSize);

            // Ne dessiner que si dans les limites du monde
            if (tileX >= 0 - cloudScaledSize  && tileX < Constants.WORLD_WIDTH) {
                batch.draw(
                    backgroundClouds,
                    tileX, cloudsY,
                    cloudScaledSize, cloudScaledSize
                );
            }
        }
    }

    private void updateCollectibles() {
        // Mettre à jour chaque collectible
        for (Collectible collectible : collectibles) {
            collectible.update(Gdx.graphics.getDeltaTime());
        }

        // Supprimer les collectibles complètement collectés
        for (int i = collectibles.size - 1; i >= 0; i--) {
            if (collectibles.get(i).isFullyCollected()) {
                Collectible removed = collectibles.removeIndex(i);
                removed.dispose();
                System.out.println("Collectible supprimé, score: " + playerScore);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        // METTRE À JOUR LES CONSTANTES D'ÉCRAN
        Constants.SCREEN_WIDTH = width;
        Constants.SCREEN_HEIGHT = height;

        // Recalculer l'échelle
        scaleX = (float) width / Constants.GAME_WIDTH;
        scaleY = (float) height / Constants.GAME_HEIGHT;

        // Mettre à jour la caméra UI
        uiCamera.setToOrtho(false, width, height);
    }

    @Override
    public void show() {
        System.out.println("GameScreen show()");
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        for (Platform platform : platforms) {
            platform.dispose();
        }
        for (Collectible collectible : collectibles) {
            collectible.dispose();
        }
        assets.dispose();
    }
}
