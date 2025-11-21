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
import com.vortexmakers.plateformer.network.NetworkManager;
import com.vortexmakers.plateformer.network.listeners.NetworkListener;
import com.vortexmakers.plateformer.network.messages.*;

import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen, NetworkListener {
    private final PlateformerGame game;
    private OrthographicCamera gameCamera;  // Caméra pour la logique de jeu
    private OrthographicCamera uiCamera;    // Caméra pour l'interface
    private SpriteBatch batch;

    // SYSTÈME MULTIJOUEUR
    private NetworkManager networkManager;

    // JOUEURS
    private Player localPlayer;  // Notre joueur local
    private Map<Integer, Player> remotePlayers;  // Joueurs distants

    // Données du joueur local
    private int localPlayerId = -1;

    // TIMING RÉSEAU
    private float networkUpdateTimer = 0f;

    //private Player player;
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
    private Texture backgroundTrees;
    private Texture backgroundClouds;

    // Positions pour effet parallaxe
    private float cloudOffset = 0;
    private float treesOffset = 0f;

    public GameScreen(PlateformerGame game, NetworkManager networkManager) {
        System.out.println("🎮 CREATION GameScreen - ID: " + networkManager.getLocalPlayerId());
        this.game = game;
        this.networkManager = networkManager;
        this.networkManager.setNetworkListener(this);

        // RÉCUPÉRER NOTRE ID DE JOUEUR
        this.localPlayerId = networkManager.getLocalPlayerId();

        // INITIALISATION DES JOUEURS
        this.remotePlayers = new HashMap<>();

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
        this.backgroundTrees = assets.getBackgroundTrees();
        this.backgroundClouds = assets.getBackgroundClouds();

        // Création des entités
        localPlayer = new Player(50, 300);  // CRÉATION DU JOUEUR LOCAL
        //player = new Player(50, 300);
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

        renderPlayers();

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
        localPlayer.update(delta);
        // Pour les collisions avec les platform et le joueur local
        physicsSystem.checkCollisions(localPlayer, platforms);
        // Pour les collisions avec les collectibles et le joueur local
        int collectedThisFrame = physicsSystem.checkCollectibleCollisions(localPlayer, collectibles);
        if (collectedThisFrame > 0) {
            playerScore += collectedThisFrame;
            System.out.println("Score: " + playerScore + " (+" + collectedThisFrame + ")");
        }

        // METTRE À JOUR LES JOUEURS DISTANTS ===========
        updateRemotePlayers(delta);

        // ENVOYER LES INPUTS AU SERVEUR ================
        sendNetworkUpdates(delta);
    }

    private void updateRemotePlayers(float delta) {
        for (Player remotePlayer : remotePlayers.values()) {
            remotePlayer.update(delta);

            // Appliquer une physique basique aux joueurs distants
            remotePlayer.applySimpleGravity();
        }
    }

    private void sendNetworkUpdates(float delta) {
        networkUpdateTimer += delta;

        if (networkUpdateTimer >= Constants.NETWORK_UPDATE_INTERVAL) {
            networkUpdateTimer = 0f;

            // ENVOYER LES INPUTS DU JOUEUR LOCAL =======
            if (networkManager.isConnected()) {
                networkManager.sendPlayerInput(
                    localPlayer.getVelocity().x,
                    localPlayer.isJumping(),
                    localPlayer.isMovingLeft(),
                    localPlayer.isMovingRight(),
                    localPlayer.getPosition().x,
                    localPlayer.getPosition().y
                );
            }
        }
    }

    private void updateCamera() {
        // SUIVI SIMPLE DU JOUEUR
        float targetX = localPlayer.getPosition().x + Constants.CAMERA_LEAD;

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

    private void renderPlayers() {
        // DESSINER LE JOUEUR LOCAL
        localPlayer.render(batch);

        // DESSINER LES JOUEURS DISTANTS
        for (Player remotePlayer : remotePlayers.values()) {
            remotePlayer.render(batch);
        }
    }

    @Override
    public void onPlayerJoined(PlayerJoinMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("Joueur rejoint: " + message.playerName + " (ID: " + message.playerId + ")");

            // Ne pas créer de joueur pour nous-même
            if (message.playerId == localPlayerId) {
                return;
            }

            // Créer un nouveau joueur distant
            Player remotePlayer = new Player(message.startX, message.startY);
            remotePlayers.put(message.playerId, remotePlayer);

            System.out.println("Nombre de joueurs distants: " + remotePlayers.size());
        });
    }

    @Override
    public void onPlayerLeft(PlayerLeaveMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("👋 Joueur parti: " + message.playerId);

            // Supprimer le joueur distant
            remotePlayers.remove(message.playerId);

            System.out.println("Nombre de joueurs distants: " + remotePlayers.size());
        });
    }

    @Override
    public void onGameStateReceived(GameStateMessage message) {
        Gdx.app.postRunnable(() -> {
            // Mettre à jour tous les joueurs distants
            for (Map.Entry<Integer, GameStateMessage.PlayerData> entry : message.playerStates.entrySet()) {
                int playerId = entry.getKey();
                GameStateMessage.PlayerData playerData = entry.getValue();

                // Ignorer notre propre joueur
                if (playerId == localPlayerId) {
                    continue;
                }

                // Mettre à jour le joueur distant
                Player remotePlayer = remotePlayers.get(playerId);
                if (remotePlayer != null) {
                    // INTERPOLATION SIMPLE DES POSITIONS
                    remotePlayer.setPosition(playerData.x, playerData.y);
                    remotePlayer.setVelocity(playerData.velocityX, playerData.velocityY);
                    remotePlayer.setGrounded(playerData.isGrounded);
                }
            }
        });
    }

    @Override
    public void onConnectedToServer() {
        // Connexion établie - pas d'action nécessaire ici
    }

    @Override
    public void onDisconnectedFromServer() {
        Gdx.app.postRunnable(() -> {
            System.out.println("Déconnecté du serveur, retour au menu");
            // Nettoyer les joueurs distants
            remotePlayers.clear();

            // Retourner au menu principal
            game.setScreen(new LobbyScreen(game));
        });
    }

    @Override
    public void onConnectionFailed(String reason) {
        Gdx.app.postRunnable(() -> {
            System.out.println("Échec connexion: " + reason);
            // Retour au menu en cas d'erreur
            game.setScreen(new LobbyScreen(game));
        });
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
        System.out.println("🎮 GameScreen SHOW() - Connecté: " + networkManager.isConnected());
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
        // Nettoyage des ressources
        batch.dispose();
        localPlayer.dispose();

        // Nettoyer les joueurs distants
        for (Player remotePlayer : remotePlayers.values()) {
            remotePlayer.dispose();
        }
        remotePlayers.clear();

        for (Platform platform : platforms) {
            platform.dispose();
        }
        for (Collectible collectible : collectibles) {
            collectible.dispose();
        }
        assets.dispose();

        // Fermer la connexion réseau
        if (networkManager != null) {
            networkManager.disconnect();
        }
    }
}
