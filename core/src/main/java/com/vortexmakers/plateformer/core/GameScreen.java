package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.vortexmakers.plateformer.entities.Spike;
import com.vortexmakers.plateformer.entities.Collectible;
import com.vortexmakers.plateformer.entities.FinishFlag;
import com.vortexmakers.plateformer.entities.Player;
import com.vortexmakers.plateformer.entities.Platform;
import com.vortexmakers.plateformer.systems.PhysicsSystem;
import com.vortexmakers.plateformer.utils.Constants;
import com.vortexmakers.plateformer.utils.AssetManager;
import com.vortexmakers.plateformer.network.NetworkManager;
import com.vortexmakers.plateformer.network.listeners.NetworkListener;
import com.vortexmakers.plateformer.network.messages.*;

import java.util.*;

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
    // MAP : PlayerID → Type de personnage
    private Map<Integer, String> playerCharacters;

    // Données du joueur local
    private int localPlayerId = -1;

    // TIMING RÉSEAU
    private float networkUpdateTimer = 0f;

    //private Player player;
    // Score du joueur
    private int playerScore;

    // Utiliser Map pour les plateformes
    private Map<Integer, Platform> clientPlatforms;
    private boolean jumpInputCaptured = false;
    private int nextPlatformId = 0;

    // COLLECTIBLES CLIENT
    private Map<Integer, Collectible> clientCollectibles;
    // SPIKES CLIENT
    private Map<Integer, Spike> clientSpikes;

    // DRAPEAU DE FIN
    private FinishFlag finishFlag;
    private Set<Integer> playersWhoFinishedLevel;
    private boolean allPlayersFinished = false;

    // SÉQUENCE D'INPUT POUR LA RÉCONCILIATION
    private int inputSequence = 0;

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

    private boolean platformsReceived = false;
    private boolean gameReady = false;

    // Le timer vient du serveur maintenant
    private float gameTimer = 0f;
    private float levelTimeLimit = 180f; // Valeur par défaut, sera écrasée par le serveur
    private boolean timerStarted = false;
    private BitmapFont uiFont;
    private int localPlayerScore = 0;
    private Map<Integer, Integer> remotePlayerScores; // Score de chaque joueur distant

    /// Transition vers les écrans de victoire où de défaite
    private boolean transitioningToWin = false;
    private boolean transitioningToGameOver = false;

    private List<PlayerJoinMessage> pendingJoinMessages;

    // INVINCIBILITÉ APRÈS RESPAWN
    private boolean localPlayerInvincible = false;
    private float invincibilityTimer = 0f;
    private static final float INVINCIBILITY_DURATION = 2.0f; // 2 secondes

    public GameScreen(PlateformerGame game, NetworkManager networkManager, String selectedCharacter) {
        System.out.println("🎮 CREATION GameScreen - ID: " + networkManager.getLocalPlayerId() + ", Personnage: " + selectedCharacter);
        this.game = game;
        this.networkManager = NetworkManager.getInstance();
        this.networkManager.setNetworkListener(this);

        // Réinitialiser les flags
        this.transitioningToWin = false;
        this.transitioningToGameOver = false;

        // RÉCUPÉRER NOTRE ID
        this.localPlayerId = networkManager.getLocalPlayerId();

        // INITIALISATION DES JOUEURS
        this.remotePlayers = new HashMap<>();

        // File d'attente pour messages reçus avant notre ID
        this.pendingJoinMessages = new ArrayList<>();

        // Initialiser la map des personnages
        this.playerCharacters = new HashMap<>();
        this.playerCharacters.put(localPlayerId, selectedCharacter);

        // Initialiser les scores
        this.remotePlayerScores = new HashMap<>();

        // CHARGER LES ASSETS AVANT TOUT
        this.assets = AssetManager.getInstance();
        if (!assets.areAssetsLoaded()) {
            assets.loadAssets(); // CHARGEMENT DES ASSETS (IMPORTANT)
            System.out.println("Assets chargés dans GameScreen");
        }

        // Initialiser les caméras
        gameCamera = new OrthographicCamera();
        gameCamera.setToOrtho(false, Constants.GAME_WIDTH, Constants.GAME_HEIGHT);
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);

        batch = new SpriteBatch();

        // Créer la police pour l'UI
        uiFont = new BitmapFont();
        uiFont.getData().setScale(2.0f);

        // Récuperation de textures du background
        this.backgroundSky = assets.getBackgroundSky();
        this.backgroundTrees = assets.getBackgroundTrees();
        this.backgroundClouds = assets.getBackgroundClouds();

        // Créer le joueur APRÈS le chargement des assets
        localPlayer = new Player(50, 300, selectedCharacter);

        // Initialiser la Map
        this.clientPlatforms = new HashMap<>();
        this.clientCollectibles = new HashMap<>();
        this.clientSpikes = new HashMap<>();

        // Initialiser
        this.playersWhoFinishedLevel = new HashSet<>();
        // Le drapeau sera créé quand on recevra sa position du serveur


        playerScore = 0;

        physicsSystem = new PhysicsSystem();
    }

    @Override
    public void render(float delta) {
        // Vérifier connexion
        if (!networkManager.isConnected()) {
            System.out.println("Plus connecté au serveur, retour au menu...");
            game.setScreen(new LobbyScreen(game));
            return;
        }

        // Attendre que les plateformes soient reçues
        if (!gameReady) {
            // Afficher un écran de chargement simple
            batch.begin();
            // Vous pouvez dessiner "Chargement..." ici si vous voulez
            batch.end();
            return;
        }

        updateBackground(delta);

        // Mise à jour
        update(delta);

        // Mise à jour de la caméra
        updateCamera();

        // Rendu (avec la caméra de jeu)
        batch.setProjectionMatrix(gameCamera.combined);
        batch.begin();

        // DESSIN DU BACKGROUND
        renderBackground();

        // Dessiner les plateformes depuis la Map
        renderPlatforms();

        renderCollectibles();
        renderSpikes();

        // Dessiner le drapeau
        if (finishFlag != null) {
            finishFlag.render(batch);
        }

        renderPlayers();

        batch.end();

        // RENDU DE L'UI (par-dessus tout)
        renderUI();

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

    /**
     * METTRE À JOUR LE JOUEUR LOCAL (inputs seulement)
     */
    private void update(float delta) {
        // Mise à jour du timer d'invincibilité
        if (localPlayerInvincible) {
            invincibilityTimer -= delta;
            if (invincibilityTimer <= 0) {
                localPlayerInvincible = false;
                System.out.println("[CLIENT] Invincibilité terminée");
            }
        }

        // Vérifier si le temps est écoulé
        if (gameTimer >= levelTimeLimit && !transitioningToGameOver && !transitioningToWin) {
            transitioningToGameOver = true;
            System.out.println("Temps écoulé ! Game Over");

            // Petit délai avant transition
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Gdx.app.postRunnable(() -> {
                        game.setScreen(new GameOverScreen(game, localPlayerScore, remotePlayerScores, gameTimer));
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            return; // Arrêter la mise à jour
        }

        // Mettre à jour le drapeau
        if (finishFlag != null) {
            finishFlag.update(delta);
        }

        // Mettre à jour l'animation
        localPlayer.updateAnimation(delta);

        // Envoyer les inputs
        sendNetworkUpdates(delta);

        // Mettre à jour les joueurs distants
        updateRemotePlayers(delta);
    }

    private void updateRemotePlayers(float delta) {
        for (Player remotePlayer : remotePlayers.values()) {
            remotePlayer.updateAnimation(delta);
        }
    }

    /**
     * ENVOYER LES INPUTS BRUTS AU SERVEUR
     */
    private void sendNetworkUpdates(float delta) {
        // Capturer le saut CHAQUE FRAME (pas seulement au moment d'envoyer)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            jumpInputCaptured = true;
        }

        networkUpdateTimer += delta;

        if (networkUpdateTimer >= Constants.NETWORK_UPDATE_INTERVAL) {
            networkUpdateTimer = 0f;

            if (networkManager.isConnected()) {
                // RÉCUPÉRER LES INPUTS BRUTS
                boolean leftPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
                boolean rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);

                // Utiliser le flag capturé
                boolean jumpPressed = jumpInputCaptured;
                jumpInputCaptured = false; // Reset après envoi

                int currentSequence = inputSequence;
                inputSequence = (inputSequence + 1) % Constants.MAX_SEQUENCE;

                networkManager.sendPlayerInput(
                    leftPressed,
                    rightPressed,
                    jumpPressed,
                    currentSequence
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
        // SUPPRESSION IMMÉDIATE des collectibles complètement collectés
        Iterator<Map.Entry<Integer, Collectible>> iterator = clientCollectibles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Collectible> entry = iterator.next();
            Collectible collectible = entry.getValue();

            collectible.update(Gdx.graphics.getDeltaTime());

            // SUPPRIMER dès que l'animation est terminée
            if (collectible.isFullyCollected()) {
                collectible.dispose();
                iterator.remove();
                System.out.println("Collectible supprimé côté client");
            }
        }
    }

    private void renderPlayers() {
        // Effet visuel invincibilité
        if (localPlayerInvincible) {
            // Clignotement : alternance visible/invisible
            boolean visible = ((int)(invincibilityTimer * 10) % 2 == 0);
            if (visible) {
                // Teinte légèrement bleue pour indiquer l'invincibilité
                batch.setColor(0.5f, 0.8f, 1.0f, 0.8f);
                localPlayer.render(batch);
                batch.setColor(1, 1, 1, 1); // Réinitialiser
            }
            // Si !visible, ne pas dessiner (effet clignotement)
        } else {
            // Normal
            localPlayer.render(batch);
        }

        // DESSINER LES JOUEURS DISTANTS (normalement)
        for (Player remotePlayer : remotePlayers.values()) {
            remotePlayer.render(batch);
        }
    }

    /**
     * RENDU DES SPIKES
     */
    private void renderSpikes() {
        for (Spike spike : clientSpikes.values()) {
            spike.render(batch);
        }
    }

    /**
     * RENDU DE L'UI (Timer, Scores)
     */
    private void renderUI() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // TIMER AU CENTRE EN HAUT
        String timerText;
        if (!timerStarted) {
            timerText = "En attente...";
        } else {
            int timeRemaining = (int) (levelTimeLimit - gameTimer);
            if (timeRemaining < 0) timeRemaining = 0; // Éviter les négatifs
            timerText = formatTime(timeRemaining);
        }

        float timerX = (Constants.SCREEN_WIDTH - timerText.length() * 15) / 2f;
        uiFont.draw(batch, timerText, timerX, Constants.SCREEN_HEIGHT - 20);

        // SCORE DU JOUEUR LOCAL
        String localScoreText = "You: " + localPlayerScore + " coins";
        uiFont.draw(batch, localScoreText, 20, Constants.SCREEN_HEIGHT - 20);

        // SCORES DES JOUEURS DISTANTS
        int yOffset = 0;
        for (Map.Entry<Integer, Integer> entry : remotePlayerScores.entrySet()) {
            String remoteScoreText = "Player " + entry.getKey() + ": " + entry.getValue() + " coins";
            float textWidth = remoteScoreText.length() * 15;
            uiFont.draw(batch, remoteScoreText,
                Constants.SCREEN_WIDTH - textWidth - 20,
                Constants.SCREEN_HEIGHT - 20 - yOffset);
            yOffset += 30;
        }

        batch.end();
    }

    /**
     * FORMATER LE TEMPS (MM:SS)
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * CRÉER UN JOUEUR DISTANT
     */
    private void createRemotePlayer(PlayerJoinMessage message) {
        System.out.println("[CLIENT] Création joueur distant ID: " + message.playerId);

        // Utiliser le personnage du message
        String characterType = message.characterType != null ? message.characterType : "beige";
        playerCharacters.put(message.playerId, characterType);

        System.out.println("[CLIENT] Personnage : " + characterType);
        System.out.println("[CLIENT] Position : X=" + message.startX + ", Y=" + message.startY);

        Player remotePlayer = new Player(message.startX, message.startY, characterType);
        remotePlayers.put(message.playerId, remotePlayer);

        System.out.println("[CLIENT] Total joueurs distants: " + remotePlayers.size());
        System.out.println("[CLIENT] IDs joueurs distants: " + remotePlayers.keySet());
    }

    private void processPendingPlayerJoin(PlayerJoinMessage message) {
        System.out.println("[CLIENT] Traitement message en attente - Joueur ID: " + message.playerId);

        // C'est notre propre ID (déjà traité)
        if (message.playerId == localPlayerId) {
            System.out.println("[CLIENT] C'était notre propre message, ignoré");
            return;
        }

        // Le joueur existe déjà
        if (remotePlayers.containsKey(message.playerId)) {
            System.out.println("[CLIENT] Joueur " + message.playerId + " existe déjà, ignoré");
            return;
        }

        // Créer le joueur distant
        createRemotePlayer(message);
    }

    @Override
    public void onPlayerJoined(PlayerJoinMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println(" [CLIENT " + localPlayerId + "] Joueur rejoint: " + message.playerName + " (ID: " + message.playerId + ")");
            System.out.println("   → Personnage reçu: " + message.characterType);

            // CAS 1 : On n'a pas encore notre ID
            if (localPlayerId == -1) {
                // Si c'est un message avec un ID valide, c'est probablement NOTRE confirmation
                if (message.playerId != -1) {
                    localPlayerId = message.playerId;
                    System.out.println("[CLIENT] Confirmation de notre ID: " + message.playerId);

                    // Sauvegarder NOTRE personnage
                    if (message.characterType != null) {
                        playerCharacters.put(message.playerId, message.characterType);
                        System.out.println("[CLIENT] Notre personnage confirmé : " + message.characterType);
                    }

                    // Traiter les messages en attente
                    if (!pendingJoinMessages.isEmpty()) {
                        System.out.println("[CLIENT] Traitement de " + pendingJoinMessages.size() + " messages en attente");
                        for (PlayerJoinMessage pendingMsg : pendingJoinMessages) {
                            processPendingPlayerJoin(pendingMsg);
                        }
                        pendingJoinMessages.clear();
                    }
                    return;
                } else {
                    // Message sans ID valide et on n'a pas encore notre ID → Mettre en attente
                    System.out.println("[CLIENT] Message en attente (pas encore d'ID local)");
                    pendingJoinMessages.add(message);
                    return;
                }
            }

            // CAS 2 : C'est notre propre ID (message en double)
            if (message.playerId == localPlayerId) {
                System.out.println("[CLIENT] Notre propre ID reçu en double, ignoré");
                return;
            }

            // CAS 3 : Le joueur existe déjà (doublon)
            if (remotePlayers.containsKey(message.playerId)) {
                System.out.println("[CLIENT] Joueur " + message.playerId + " existe déjà, ignoré");
                return;
            }

            // CAS 4 : C'est un nouveau joueur distant (valide)
            createRemotePlayer(message);
        });
    }

    @Override
    public void onPlayerLeft(PlayerLeaveMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("Joueur parti: " + message.playerId);

            // Supprimer le joueur distant
            remotePlayers.remove(message.playerId);

            System.out.println("Nombre de joueurs distants: " + remotePlayers.size());
        });
    }

    /**
     * RÉCEPTION D'UN CHANGEMENT DE PERSONNAGE
     */
    @Override
    public void onPlayerCharacterChanged(PlayerCharacterMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[CLIENT] Joueur " + message.playerId + " choisit : " + message.characterType);

            // Sauvegarder le choix
            playerCharacters.put(message.playerId, message.characterType);

            // Si c'est un joueur distant qui existe déjà, le recréer avec le bon personnage
            if (message.playerId != localPlayerId) {
                Player remotePlayer = remotePlayers.get(message.playerId);
                if (remotePlayer != null) {
                    // Sauvegarder la position actuelle
                    float x = remotePlayer.getPosition().x;
                    float y = remotePlayer.getPosition().y;

                    // Recréer avec le nouveau personnage
                    remotePlayer.dispose();
                    remotePlayers.put(message.playerId, new Player(x, y, message.characterType));

                    System.out.println("[CLIENT] Joueur distant " + message.playerId + " recréé avec personnage : " + message.characterType);
                }
            }
        });
    }

    /**
     * RÉCEPTION DE L'ÉTAT DU JEU DU SERVEUR - Version corrigée
     */
    @Override
    public void onGameStateReceived(GameStateMessage message) {
        Gdx.app.postRunnable(() -> {
            for (Map.Entry<Integer, GameStateMessage.PlayerData> entry : message.playerStates.entrySet()) {
                int playerId = entry.getKey();
                GameStateMessage.PlayerData playerData = entry.getValue();

                if (playerId == localPlayerId) {
                    // FORCER la mise à jour de l'état grounded pour l'animation
                    localPlayer.setGrounded(playerData.isGrounded);
                    localPlayer.setPosition(playerData.x, playerData.y);
                    localPlayer.setVelocity(playerData.velocityX, playerData.velocityY);
                } else {
                    Player remotePlayer = remotePlayers.get(playerId);
                    if (remotePlayer != null) {
                        remotePlayer.setGrounded(playerData.isGrounded);
                        remotePlayer.setPosition(playerData.x, playerData.y);
                        remotePlayer.setVelocity(playerData.velocityX, playerData.velocityY);
                    }
                }
            }
        });
    }

    @Override
    public void onPlatformStateReceived(PlatformStateMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("CLIENT: Réception " + message.platforms.size() + " plateformes");

            clientPlatforms.clear();

            for (int i = 0; i < message.platforms.size(); i++) {
                PlatformStateMessage.PlatformData platformData = message.platforms.get(i);
                Platform platform;

                if (platformData.height == Constants.PLATFORM_HEIGHT) {
                    platform = new Platform(platformData.x, platformData.y, platformData.width);
                } else {
                    platform = new Platform(platformData.x, platformData.y, platformData.width, platformData.height);
                }

                clientPlatforms.put(i, platform);
            }

            // Marquer que les plateformes sont reçues
            platformsReceived = true;
            gameReady = true;

            System.out.println(" " + clientPlatforms.size() + " plateformes créées - Jeu prêt !");
        });
    }

    /**
     * RÉCEPTION DE L'ÉTAT DES COLLECTIBLES DU SERVEUR
     */
    @Override
    public void onCollectibleStateReceived(CollectibleStateMessage message) {
        Gdx.app.postRunnable(() -> {
            for (CollectibleStateMessage.CollectibleData collectibleData : message.collectibles) {
                Collectible collectible = clientCollectibles.get(collectibleData.collectibleId);

                if (collectible == null && !collectibleData.collected) {
                    // CRÉATION si pas encore collecté
                    collectible = new Collectible(collectibleData.x, collectibleData.y);
                    clientCollectibles.put(collectibleData.collectibleId, collectible);
                }

                if (collectible != null && collectibleData.collected) {
                    // FORCER la collecte immédiate
                    if (!collectible.isFullyCollected() && !collectible.isCollecting()) {
                        collectible.collect();

                        // Mettre à jour le score
                        if (collectibleData.collectedByPlayerId == localPlayerId) {
                            localPlayerScore++;
                            System.out.println("Score local: " + localPlayerScore);
                        } else {
                            remotePlayerScores.put(
                                collectibleData.collectedByPlayerId,
                                remotePlayerScores.getOrDefault(collectibleData.collectedByPlayerId, 0) + 1
                            );
                            System.out.println("Score joueur " + collectibleData.collectedByPlayerId + ": " + remotePlayerScores.get(collectibleData.collectedByPlayerId));
                        }
                    }
                }
            }
        });
    }

    /**
     * RÉCEPTION DE L'ÉTAT DES SPIKES DU SERVEUR
     */
    @Override
    public void onSpikeStateReceived(SpikeStateMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[CLIENT] Réception " + message.spikes.size() + " spikes");

            clientSpikes.clear();

            for (int i = 0; i < message.spikes.size(); i++) {
                SpikeStateMessage.SpikeData spikeData = message.spikes.get(i);
                Spike spike = new Spike(spikeData.x, spikeData.y);
                clientSpikes.put(i, spike);
            }

            System.out.println("" + clientSpikes.size() + " spikes créés côté client");
        });
    }

    /**
     * RÉCEPTION D'UN MESSAGE DE RESPAWN
     */
    @Override
    public void onPlayerRespawned(PlayerRespawnMessage message) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[CLIENT] Joueur " + message.playerId + " respawn à X: " + message.spawnX + ", Y: " + message.spawnY);

            if (message.playerId == localPlayerId) {
                // C'est NOTRE respawn
                localPlayer.setPosition(message.spawnX, message.spawnY);
                localPlayer.setVelocity(0, 0);
                localPlayer.setGrounded(false);

                // Activer invincibilité
                if (message.invincible) {
                    localPlayerInvincible = true;
                    invincibilityTimer = INVINCIBILITY_DURATION;
                    System.out.println("[CLIENT] Invincibilité activée pour " + INVINCIBILITY_DURATION + "s");
                }
            } else {
                // C'est un joueur distant
                Player remotePlayer = remotePlayers.get(message.playerId);
                if (remotePlayer != null) {
                    remotePlayer.setPosition(message.spawnX, message.spawnY);
                    remotePlayer.setVelocity(0, 0);
                    remotePlayer.setGrounded(false);
                }
            }
        });
    }

    /**
     * RÉCEPTION DE L'ÉTAT DU DRAPEAU
     */
    @Override
    public void onFinishFlagStateReceived(FinishFlagStateMessage message) {
        Gdx.app.postRunnable(() -> {
            //  Vérifier l'état des flags
            System.out.println("[CLIENT] onFinishFlagStateReceived - allPlayersFinished: " + message.allPlayersFinished +
                ", transitioningToWin: " + transitioningToWin);

            // Créer le drapeau si pas encore fait
            if (finishFlag == null) {
                finishFlag = new FinishFlag(message.flagX, message.flagY);
                System.out.println("[CLIENT] Drapeau créé à X: " + message.flagX + ", Y: " + message.flagY);
            }

            // Mettre à jour la liste des joueurs qui ont fini
            playersWhoFinishedLevel = new HashSet<>(message.playersWhoFinished);
            allPlayersFinished = message.allPlayersFinished;

            // Log si on a fini
            if (playersWhoFinishedLevel.contains(localPlayerId)) {
                System.out.println("[CLIENT] Vous avez atteint le drapeau !");
            }

            // Si tous ont fini, passer à l'écran de victoire
            if (allPlayersFinished && !transitioningToWin) {
                transitioningToWin = true;
                System.out.println("[CLIENT] TOUS LES JOUEURS ONT FINI ! Transition vers LevelWinScreen...");

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Gdx.app.postRunnable(() -> {
                            game.setScreen(new LevelWinScreen(game, localPlayerScore, remotePlayerScores));
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    /**
     * RÉCEPTION DU TIMER DU SERVEUR
     */
    @Override
    public void onGameTimerReceived(GameTimerMessage message) {
        Gdx.app.postRunnable(() -> {
            // SYNCHRONISER avec le serveur
            gameTimer = message.currentTime;
            levelTimeLimit = message.timeLimit;
            timerStarted = message.timerStarted;

            // Debug occasionnel
            if ((int)gameTimer % 10 == 0) {
                System.out.println("Timer synchronisé: " + formatTime((int)(levelTimeLimit - gameTimer)));
            }
        });
    }

    /**
     * RENDU DES PLATEFORMES
     */
    private void renderPlatforms() {
        for (Platform platform : clientPlatforms.values()) {
            platform.render(batch);
        }
    }

    /**
     * RENDU DES COLLECTIBLES
     */
    private void renderCollectibles() {
        for (Collectible collectible : clientCollectibles.values()) {
            collectible.render(batch);
        }
    }

    @Override
    public void onConnectedToServer() {
        // Connexion établie - pas d'action nécessaire ici
    }

    @Override
    public void onDisconnectedFromServer() {
        Gdx.app.postRunnable(() -> {
            System.out.println("🔌 Déconnecté du serveur, retour au menu");

            // Nettoyer les joueurs distants
            remotePlayers.clear();
            clientCollectibles.clear();

            // Attendre un peu avant de retourner au lobby
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Petit délai pour voir le message
                    Gdx.app.postRunnable(() -> {
                        game.setScreen(new LobbyScreen(game));
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
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

    /**
     * RÉCEPTION DE L'ÉTAT DU LOBBY (pas utilisé en jeu)
     */
    @Override
    public void onLobbyStateReceived(LobbyStateMessage message) {
        // Pas utilisé dans GameScreen, seulement dans LobbyScreen
    }

    @Override
    public void onPlayerReadyStateReceived(LobbyStateMessage message) {}

    @Override
    public void onGameStartReceived() {}

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
        System.out.println("🎮 GameScreen SHOW() - Connecté: " + networkManager.isConnected() + ", Host: " + networkManager.isHost());
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
        // Nettoyage des ressources graphiques
        if (batch != null) {
            batch.dispose();
            batch = null;
        }

        // Disposer la police UI
        if (uiFont != null) {
            uiFont.dispose();
            uiFont = null;
        }

        // Nettoyage du joueur local
        if (localPlayer != null) {
            localPlayer.dispose();
            localPlayer = null;
        }

        // Nettoyer les joueurs distants
        for (Player remotePlayer : remotePlayers.values()) {
            if (remotePlayer != null) {
                remotePlayer.dispose();
            }
        }
        remotePlayers.clear();

        if (pendingJoinMessages != null) {
            pendingJoinMessages.clear();
        }

        // Nettoyer les plateformes
        for (Platform platform : clientPlatforms.values()) {
            if (platform != null) {
                platform.dispose();
            }
        }
        clientPlatforms.clear();

        // Nettoyer les collectibles
        for (Collectible collectible : clientCollectibles.values()) {
            if (collectible != null) {
                collectible.dispose();
            }
        }
        clientCollectibles.clear();

        // Nettoyer les spikes
        for (Spike spike : clientSpikes.values()) {
            if (spike != null) {
                spike.dispose();
            }
        }
        clientSpikes.clear();

        // Nettoyer le drapeau
        if (finishFlag != null) {
            finishFlag.dispose();
            finishFlag = null;
        }

        transitioningToWin = false;
        transitioningToGameOver = false;

        System.out.println("GameScreen nettoyé");
    }
}
