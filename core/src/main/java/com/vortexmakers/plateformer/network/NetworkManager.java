package com.vortexmakers.plateformer.network;

// IMPORTATIONS KRYONET ==================================
import com.badlogic.gdx.math.Rectangle;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

// IMPORTATIONS MESSAGES =================================
import com.vortexmakers.plateformer.network.listeners.NetworkListener;
import com.vortexmakers.plateformer.network.messages.*;
import com.vortexmakers.plateformer.utils.Constants;

// IMPORTATIONS JAVA =====================================
import java.io.IOException;
import java.util.*;

/**
 * NETWORKMANAGER - Gestionnaire réseau principal
 *
 * Peut fonctionner en mode :
 * - SERVEUR (Host) : Héberge la partie, fait autorité
 * - CLIENT : Se connecte à un serveur
 */
public class NetworkManager {
    // INSTANCE UNIQUE
    private static NetworkManager instance;

    // CONFIGURATION RÉSEAU
    private static final int TCP_PORT = 54555;   // Port TCP pour données fiables
    private static final int UDP_PORT = 54777;   // Port UDP pour données rapides

    // RÉFÉRENCES RÉSEAU
    private Server server;
    private Client client;

    // ÉTATS
    private boolean isHost = false;
    private boolean isConnected = false;
    private NetworkListener networkListener;

    // DONNÉES JOUEURS
    private int localPlayerId = -1;  // Notre ID (assigné par le serveur)
    private String localPlayerName = "Player";
    private String localPlayerCharacter = "beige"; // Personnage choisi localement

    // POUR LE SERVEUR AUTORITAIRE
    private Map<Integer, ServerPlayer> serverPlayers;
    private List<CollectibleStateMessage.CollectibleData> serverCollectibles;
    // Utiliser PlatformData au lieu de Rectangle
    private List<PlatformStateMessage.PlatformData> serverPlatforms;
    private int nextCollectibleId = 0;

    // SPIKES SERVEUR
    private List<SpikeStateMessage.SpikeData> serverSpikes;

    private boolean initialStateSent = false;
    // TIMING SERVEUR
    private float serverUpdateTimer = 0f;

    // Pour le serveur : suivre tous les joueurs
    private Map<Integer, GameStateMessage.PlayerData> connectedPlayers;
    // Pour suivre la dernière séquence reçue de chaque joueur
    private Map<Integer, Integer> lastReceivedSequence;
    // MAP : PlayerID → Type de personnage
    private Map<Integer, String> playerCharacters;

    // Timer du jeu (géré par le serveur)
    private float serverGameTimer = 0f;
    private float serverTimeLimit = 180f; // 3 minutes
    private boolean serverTimerStarted = false;

    // Drapeau de fin
    private float finishFlagX = 2800f; // Position X du drapeau (près de la fin du monde)
    private float finishFlagY = 32f;   // Position Y (sur le sol)
    private Set<Integer> playersWhoFinished = new HashSet<>();

    // CONSTRUCTEUR PRIVÉ
    private NetworkManager() {
        this.connectedPlayers = new HashMap<>();
        this.serverPlayers = new HashMap<>();
        this.playerCharacters = new HashMap<>();
        this.serverCollectibles = new ArrayList<>();
        this.serverPlatforms = new ArrayList<>();
        this.lastReceivedSequence = new HashMap<>();
        this.serverSpikes = new ArrayList<>();
        System.out.println("NetworkManager créé (Singleton)");
    }

    // POINT D'ACCÈS GLOBAL
    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    // MÉTHODE POUR RÉINITIALISER (optionnel)
    public static void resetInstance() {
        if (instance != null) {
            instance.disconnect();
            instance = null;
        }
    }

    /**
     * DÉFINIR LE PERSONNAGE CHOISI LOCALEMENT (avant connexion)
     */
    public void setLocalCharacter(String characterType) {
        this.localPlayerCharacter = characterType;
        System.out.println("[CLIENT] Personnage local défini : " + characterType);
    }

    /**
     * DÉMARRER EN MODE HOST (Serveur SEULEMENT)
     */
    public void startHost() {
        System.out.println("Démarrage du serveur...");

        isHost = true;

        try {
            // CRÉATION SERVEUR
            server = new Server();

            // ENREGISTREMENT DES CLASSES
            registerClasses(server.getKryo());

            // DÉMARRAGE SERVEUR
            server.start();
            server.bind(TCP_PORT, UDP_PORT);

            // ÉCOUTEUR SERVEUR
            server.addListener(new Listener() {
                @Override
                public void connected(Connection connection) {
                    System.out.println("Client connecté: " + connection.getID() + " from " + connection.getRemoteAddressTCP());
                }

                @Override
                public void disconnected(Connection connection) {
                    System.out.println("Client déconnecté: " + connection.getID());
                    handlePlayerDisconnected(connection.getID());
                }

                @Override
                public void received(Connection connection, Object object) {
                    handleServerMessage(connection, object);
                }
            });

            System.out.println("Serveur démarré sur port " + TCP_PORT + " - En attente de joueurs...");

            // Le host doit maintenant se connecter en client
            // Mais on laisse le LobbyScreen gérer cette connexion
            System.out.println("Host - Prêt à se connecter en client");

            // INITIALISATION DU MONDE SERVEUR ==========
            initializeServerWorld();

            System.out.println("Serveur autoritaire démarré");

        } catch (IOException e) {
            System.err.println("Erreur démarrage serveur: " + e.getMessage());
            if (networkListener != null) {
                networkListener.onConnectionFailed(e.getMessage());
            }
        }
    }

    /**
     * INITIALISATION DU MONDE SERVEUR
     */
    private void initializeServerWorld() {
        // CRÉATION DES COLLECTIBLES SERVEUR
        createServerCollectibles();

        // CRÉATION DES PLATFORM SERVEUR
        createServerPlatforms();

        // CRÉATION DES SPIKES SERVEUR
        createServerSpikes();

        // Initialiser le set de joueurs finis
        playersWhoFinished = new HashSet<>();

        System.out.println("Monde serveur initialisé avec " + serverCollectibles.size() + " collectibles et " + serverPlatforms.size() + " plateformes");
        System.out.println("Drapeau placé à X: " + finishFlagX + ", Y: " + finishFlagY);


        System.out.println("Monde serveur initialisé avec " + serverCollectibles.size() + " collectibles et " + serverPlatforms.size() + " plateformes");
        System.out.println("Drapeau placé à X: " + finishFlagX + ", Y: " + finishFlagY);
    }

    /**
     * FAIRE RESPAWN UN JOUEUR
     */
    private void respawnPlayer(ServerPlayer serverPlayer) {
        // Position de spawn (début du niveau)
        float spawnX = 50f;
        float spawnY = 300f;

        // Téléporter le joueur
        serverPlayer.x = spawnX;
        serverPlayer.y = spawnY;
        serverPlayer.velocityX = 0;
        serverPlayer.velocityY = 0;
        serverPlayer.isGrounded = false;
        serverPlayer.updateBounds();

        System.out.println("[SERVEUR] Joueur " + serverPlayer.playerId + " respawn à X: " + spawnX + ", Y: " + spawnY);

        // Envoyer message de respawn à tous les clients
        PlayerRespawnMessage respawnMessage = new PlayerRespawnMessage(
            serverPlayer.playerId,
            spawnX,
            spawnY,
            true // Invincible pendant 2 secondes
        );

        if (server != null) {
            server.sendToAllTCP(respawnMessage);
        }
    }


    /**
     * CRÉATION DES PLATEFORMES SUR LE SERVEUR
     */
    private void createServerPlatforms() {
        serverPlatforms.clear(); // S'assurer que c'est vide

        // UTILISER PlatformData DIRECTEMENT
        // Plateforme de base (sol)
        for (int i = 0; i < 15; i++) {
            serverPlatforms.add(new PlatformStateMessage.PlatformData(
                i * 200, 0, 160, Constants.PLATFORM_HEIGHT, 0 // type 0 = terrain
            ));
        }

        // Quelques plateformes
        serverPlatforms.add(new PlatformStateMessage.PlatformData(200, 80, 100, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(580, 150, 100, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(100, 160, 85, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(720, 80, 120, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(500, Constants.PLATFORM_HEIGHT, 32, 80, 1)); // type 1 = block

        // Nouvelle plateforme loin à droite
        serverPlatforms.add(new PlatformStateMessage.PlatformData(1150, 80, 400, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(1400, 160, 100, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(1700, 160, 150, Constants.PLATFORM_HEIGHT, 0));

        // Ajoutons encore plus de plateformes
        serverPlatforms.add(new PlatformStateMessage.PlatformData(2000, 200, 200, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(2300, 100, 150, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(2600, 150, 120, Constants.PLATFORM_HEIGHT, 0));
        serverPlatforms.add(new PlatformStateMessage.PlatformData(2900, 80, 200, Constants.PLATFORM_HEIGHT, 0));

        System.out.println(" " + serverPlatforms.size() + " plateformes créées sur le serveur");
    }

    /**
     * CRÉATION DES SPIKES SUR LE SERVEUR
     */
    private void createServerSpikes() {
        serverSpikes.clear();

        // Quelques spikes
        serverSpikes.add(new SpikeStateMessage.SpikeData(400, 32));
        serverSpikes.add(new SpikeStateMessage.SpikeData(800, 32));
        serverSpikes.add(new SpikeStateMessage.SpikeData(1300, 32));
        serverSpikes.add(new SpikeStateMessage.SpikeData(1600, 32));
        serverSpikes.add(new SpikeStateMessage.SpikeData(2200, 32));
        serverSpikes.add(new SpikeStateMessage.SpikeData(600, 32));
        serverSpikes.add(new SpikeStateMessage.SpikeData(1000, 32));
        serverSpikes.add(new SpikeStateMessage.SpikeData(1800, 32));

        System.out.println("" + serverSpikes.size() + " spikes créés sur le serveur");
    }

    /**
     * CRÉATION DES COLLECTIBLES SUR LE SERVEUR
     */
    private void createServerCollectibles() {
        // Mêmes positions que dans GameScreen.createCollectibles()
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 250, 120, false, -1));
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 600, 180, false, -1));
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 780, 100, false, -1));
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 1200, 220, false, -1));
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 1700, 220, false, -1));
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 2050, 250, false, -1));
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 400, 250, false, -1));
        serverCollectibles.add(new CollectibleStateMessage.CollectibleData(nextCollectibleId++, 1400, 210, false, -1));
    }

    /**
     * MISE À JOUR DU SERVEUR - Version corrigée
     */
    public void updateServer(float delta) {
        if (!isHost || server == null) return;

        serverUpdateTimer += delta;
        if (serverUpdateTimer >= Constants.SERVER_UPDATE_INTERVAL) {
            serverUpdateTimer = 0f;

            // Toujours mettre à jour et envoyer, même sans joueurs
            if (!serverPlayers.isEmpty()) {
                // Démarrer le timer dès qu'il y a des joueurs
                if (!serverTimerStarted) {
                    serverTimerStarted = true;
                    System.out.println("[SERVEUR] Timer démarré !");
                }

                // Incrémenter le timer du serveur
                if (serverTimerStarted) {
                    serverGameTimer += Constants.SERVER_UPDATE_INTERVAL;

                    // Vérifier si le temps est écoulé
                    if (serverGameTimer >= serverTimeLimit) {
                        System.out.println("[SERVEUR] Temps écoulé ! Game Over");
                        // TODO : Envoyer message Game Over (Phase 4)
                    }
                }
                // APPLIQUER LA PHYSIQUE À TOUS LES JOUEURS
                for (ServerPlayer serverPlayer : serverPlayers.values()) {
                    serverPlayer.applyServerPhysics(Constants.SERVER_UPDATE_INTERVAL);
                    checkServerPlatformCollisions(serverPlayer);
                    checkServerCollectibleCollisions(serverPlayer);

                    // Vérifier chute dans le vide
                    checkServerFallDeath(serverPlayer);

                    // Vérifier collision avec spikes
                    checkServerSpikeCollisions(serverPlayer);

                    // Vérifier si le joueur atteint le drapeau
                    checkFinishFlagCollision(serverPlayer);
                }

                updateConnectedPlayersFromServer();
            }

            // Envoyer les états même sans joueurs (pour synchronisation initiale)
            sendGameStateToAll();
            //sendPlatformStateToAll();
            sendCollectibleStateToAll();

            // Envoyer le timer toutes les secondes
            if (serverTimerStarted && (int)serverGameTimer % 1 == 0) {
                sendTimerStateToAll();
            }
        }
    }

    /**
     * RÉSOLUTION DES COLLISIONS (côté serveur) - Version corrigée
     */
    private boolean resolveServerCollision(ServerPlayer serverPlayer, Rectangle platform) {
        Rectangle playerBounds = serverPlayer.getBounds();

        // Calcul des chevauchements
        float overlapLeft = playerBounds.x + playerBounds.width - platform.x;
        float overlapRight = platform.x + platform.width - playerBounds.x;
        float overlapTop = platform.y + platform.height - playerBounds.y;
        float overlapBottom = playerBounds.y + playerBounds.height - platform.y;

        // Vérifier chevauchements positifs
        if (overlapLeft <= 0 || overlapRight <= 0 || overlapTop <= 0 || overlapBottom <= 0) {
            return false;
        }

        // Trouver la plus petite pénétration
        float minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
            Math.min(overlapTop, overlapBottom));

        // Seuil plus strict pour éviter les collisions ambiguës
        final float COLLISION_THRESHOLD = 1.0f;

        if (minOverlap == overlapTop && overlapTop > COLLISION_THRESHOLD) {
            // COLLISION PAR LE HAUT : atterrissage
            // Positionner EXACTEMENT sur la plateforme
            serverPlayer.y = platform.y + platform.height;
            serverPlayer.velocityY = 0;
            serverPlayer.updateBounds();
            return true;

        } else if (minOverlap == overlapBottom && overlapBottom > COLLISION_THRESHOLD) {
            // COLLISION PAR LE BAS : plafond
            serverPlayer.y = platform.y - playerBounds.height;
            // Annuler seulement la vélocité positive
            if (serverPlayer.velocityY > 0) {
                serverPlayer.velocityY = 0;
            }
            serverPlayer.updateBounds();
            return false;

        } else if (minOverlap == overlapLeft && overlapLeft > COLLISION_THRESHOLD) {
            // COLLISION PAR LA GAUCHE
            serverPlayer.x = platform.x - playerBounds.width;
            serverPlayer.velocityX = 0;
            serverPlayer.updateBounds();
            return false;

        } else if (minOverlap == overlapRight && overlapRight > COLLISION_THRESHOLD) {
            // COLLISION PAR LA DROITE
            serverPlayer.x = platform.x + platform.width;
            serverPlayer.velocityX = 0;
            serverPlayer.updateBounds();
            return false;
        }

        return false;
    }

    /**
     * VÉRIFICATION DES COLLISIONS PLATEFORMES (côté serveur)
     */
    private void checkServerPlatformCollisions(ServerPlayer serverPlayer) {
        boolean grounded = false;

        // Tolérance pour éviter les micro-gaps
        final float GROUND_TOLERANCE = 2.0f; // Pixels de tolérance

        for (PlatformStateMessage.PlatformData platform : serverPlatforms) {
            Rectangle platformRect = new Rectangle(platform.x, platform.y, platform.width, platform.height);
            Rectangle playerBounds = serverPlayer.getBounds();

            // Vérifier si le joueur est "proche" du sol
            boolean isNearGround =
                playerBounds.y <= platformRect.y + platformRect.height + GROUND_TOLERANCE &&
                    playerBounds.y + playerBounds.height > platformRect.y &&
                    playerBounds.x + playerBounds.width > platformRect.x &&
                    playerBounds.x < platformRect.x + platformRect.width;

            if (playerBounds.overlaps(platformRect)) {
                boolean wasGroundedThisCollision = resolveServerCollision(serverPlayer, platformRect);
                grounded = wasGroundedThisCollision || grounded;
            } else if (isNearGround && Math.abs(serverPlayer.velocityY) < 10f) {
                // Si très proche du sol et pas beaucoup de vélocité Y,
                // considérer comme au sol (évite les micro-rebonds)
                grounded = true;
            }
        }

        // Mise à jour de l'état
        boolean wasGrounded = serverPlayer.isGrounded;
        serverPlayer.isGrounded = grounded;

        // Forcer vélocité Y à 0 si au sol
        if (grounded && serverPlayer.velocityY < 0) {
            serverPlayer.velocityY = 0;
        }

        // Logger seulement les changements significatifs (pas chaque frame)
        if (!wasGrounded && grounded) {
            System.out.println("Joueur " + serverPlayer.playerId + " atterrit");
        } else if (wasGrounded && !grounded && serverPlayer.velocityY > 10f) {
            // Logger seulement si vraiment un saut (pas un micro-gap)
            System.out.println("Joueur " + serverPlayer.playerId + " décolle");
        }
    }

    /**
     * VÉRIFICATION DES COLLISIONS COLLECTIBLES (côté serveur)
     */
    private void checkServerCollectibleCollisions(ServerPlayer serverPlayer) {
        for (CollectibleStateMessage.CollectibleData collectible : serverCollectibles) {
            if (!collectible.collected) {
                Rectangle collectibleBounds = new Rectangle(
                    collectible.x, collectible.y,
                    Constants.COLLECTIBLE_SIZE, Constants.COLLECTIBLE_SIZE
                );

                if (serverPlayer.getBounds().overlaps(collectibleBounds)) {
                    // MARQUER COMME COLLECTÉ IMMÉDIATEMENT
                    collectible.collected = true;
                    collectible.collectedByPlayerId = serverPlayer.playerId;

                    System.out.println("Serveur: Collectible " + collectible.collectibleId + " collecté");

                    // ENVOYER IMMÉDIATEMENT l'état mis à jour
                    sendCollectibleStateToAll();
                    break; // Un collectible par frame
                }
            }
        }
    }

    /**
     * VÉRIFIER SI UN JOUEUR TOMBE DANS LE VIDE
     */
    private void checkServerFallDeath(ServerPlayer serverPlayer) {
        final float DEATH_THRESHOLD = -50f; // En dessous de Y = -50 = mort

        if (serverPlayer.y < DEATH_THRESHOLD) {
            System.out.println("[SERVEUR] Joueur " + serverPlayer.playerId + " est tombé dans le vide !");
            respawnPlayer(serverPlayer);
        }
    }

    /**
     * VÉRIFIER SI UN JOUEUR TOUCHE UN SPIKE
     */
    private void checkServerSpikeCollisions(ServerPlayer serverPlayer) {
        for (SpikeStateMessage.SpikeData spike : serverSpikes) {
            // Rectangle du spike (32x32)
            Rectangle spikeBounds = new Rectangle(spike.x, spike.y, 32f, 32f);

            if (serverPlayer.getBounds().overlaps(spikeBounds)) {
                System.out.println("[SERVEUR] Joueur " + serverPlayer.playerId + " a touché un spike !");
                respawnPlayer(serverPlayer);
                break; // Un spike par frame suffit
            }
        }
    }

    /**
     * VÉRIFIER SI LE JOUEUR ATTEINT LE DRAPEAU
     */
    private void checkFinishFlagCollision(ServerPlayer serverPlayer) {
        // Zone du drapeau (64x64)
        Rectangle flagBounds = new Rectangle(finishFlagX, finishFlagY, 64f, 64f);

        if (serverPlayer.getBounds().overlaps(flagBounds)) {
            // Le joueur a atteint le drapeau
            if (!playersWhoFinished.contains(serverPlayer.playerId)) {
                playersWhoFinished.add(serverPlayer.playerId);
                System.out.println("[SERVEUR] Joueur " + serverPlayer.playerId + " a atteint le drapeau ! (" + playersWhoFinished.size() + "/" + serverPlayers.size() + ")");

                // Vérifier si tous les joueurs ont fini
                if (playersWhoFinished.size() == serverPlayers.size()) {
                    System.out.println("[SERVEUR] TOUS LES JOUEURS ONT FINI ! Victory !");
                }

                // Envoyer l'état mis à jour
                sendFinishFlagStateToAll();
            }
        }
    }

    /**
     * METTRE À JOUR LES DONNÉES CLIENTS DEPUIS LE SERVEUR - Version corrigée
     */
    private void updateConnectedPlayersFromServer() {
        for (Map.Entry<Integer, ServerPlayer> entry : serverPlayers.entrySet()) {
            int playerId = entry.getKey();
            ServerPlayer serverPlayer = entry.getValue();

            GameStateMessage.PlayerData playerData = connectedPlayers.get(playerId);
            if (playerData == null) {
                playerData = new GameStateMessage.PlayerData();
                connectedPlayers.put(playerId, playerData);
            }

            // SYNCHRONISATION COMPLÈTE
            playerData.x = serverPlayer.x;
            playerData.y = serverPlayer.y;
            playerData.velocityX = serverPlayer.velocityX;
            playerData.velocityY = serverPlayer.velocityY;
            playerData.isGrounded = serverPlayer.isGrounded;
            playerData.playerName = "Player " + playerId;
        }
    }

    /**
     * ENVOYER LE CHOIX DE PERSONNAGE AU SERVEUR
     */
    public void sendCharacterChoice(String characterType) {
        if (client != null && client.isConnected()) {
            PlayerCharacterMessage msg = new PlayerCharacterMessage(localPlayerId, characterType);
            client.sendTCP(msg);
            System.out.println("[CLIENT] Envoi choix personnage : " + characterType);
        }
    }

    /**
     * ENVOYER L'ÉTAT DES PLATEFORMES À TOUS LES CLIENTS
     */
    private void sendPlatformStateToAll() {
        try {
            PlatformStateMessage platformState = new PlatformStateMessage();
            platformState.platforms.addAll(serverPlatforms);

            if (server != null && server.getConnections().length > 0) {
                server.sendToAllTCP(platformState);
            }
        } catch (Exception e) {
            System.err.println("Erreur envoi état plateformes: " + e.getMessage());
        }
    }

    /**
     * ENVOYER L'ÉTAT DES COLLECTIBLES À TOUS LES CLIENTS
     */
    private void sendCollectibleStateToAll() {
        try {
            CollectibleStateMessage collectibleState = new CollectibleStateMessage();
            collectibleState.collectibles.addAll(serverCollectibles);

            // Vérification avant envoi
            if (server != null && server.getConnections().length > 0) {
                server.sendToAllUDP(collectibleState);
            }
        } catch (Exception e) {
            System.err.println("Erreur envoi état collectibles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ENVOYER L'ÉTAT DES SPIKES À TOUS LES CLIENTS
     */
    private void sendSpikeStateToAll() {
        try {
            SpikeStateMessage spikeState = new SpikeStateMessage();
            spikeState.spikes.addAll(serverSpikes);

            if (server != null && server.getConnections().length > 0) {
                server.sendToAllTCP(spikeState);
                System.out.println("[SERVEUR] Spikes envoyés à tous les clients");
            }
        } catch (Exception e) {
            System.err.println("Erreur envoi état spikes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ENVOYER L'ÉTAT DU DRAPEAU À TOUS LES CLIENTS
     */
    private void sendFinishFlagStateToAll() {
        try {
            boolean allFinished = (playersWhoFinished.size() == serverPlayers.size()) && !serverPlayers.isEmpty();

            FinishFlagStateMessage flagMessage = new FinishFlagStateMessage(
                finishFlagX,
                finishFlagY,
                playersWhoFinished,
                allFinished
            );

            if (server != null && server.getConnections().length > 0) {
                server.sendToAllTCP(flagMessage); // TCP pour garantir la réception
                System.out.println("[SERVEUR] État drapeau envoyé - Finis: " + playersWhoFinished.size());
            }
        } catch (Exception e) {
            System.err.println("Erreur envoi état drapeau: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ENVOYER L'ÉTAT DU TIMER À TOUS LES CLIENTS
     */
    private void sendTimerStateToAll() {
        try {
            GameTimerMessage timerMessage = new GameTimerMessage(
                serverGameTimer,
                serverTimeLimit,
                serverTimerStarted
            );

            if (server != null && server.getConnections().length > 0) {
                server.sendToAllUDP(timerMessage);
            }
        } catch (Exception e) {
            System.err.println("Erreur envoi timer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * NOUVEAU : Envoyer l'état initial à UN joueur spécifique dans l'ordre correct
     */
    private void sendInitialGameStateToPlayer(Connection connection) {
        try {
            // ÉTAPE 1 : Envoyer les plateformes (CRITIQUE)
            PlatformStateMessage platformState = new PlatformStateMessage();
            platformState.platforms.addAll(serverPlatforms);
            connection.sendTCP(platformState); // TCP pour garantir l'ordre

            System.out.println("[SERVEUR] Plateformes envoyées à joueur " + connection.getID());

            // ÉTAPE 2 : Envoyer les collectibles
            CollectibleStateMessage collectibleState = new CollectibleStateMessage();
            collectibleState.collectibles.addAll(serverCollectibles);
            connection.sendTCP(collectibleState);

            System.out.println("[SERVEUR] Collectibles envoyés à joueur " + connection.getID());

            // Envoyer les spikes
            SpikeStateMessage spikeState = new SpikeStateMessage();
            spikeState.spikes.addAll(serverSpikes);
            connection.sendTCP(spikeState);

            System.out.println("[SERVEUR] Spikes envoyés à joueur " + connection.getID());

            // ÉTAPE 3 : Envoyer l'état du jeu
            GameStateMessage gameState = new GameStateMessage();
            gameState.serverTime = System.currentTimeMillis();
            gameState.playerStates.putAll(connectedPlayers);
            connection.sendTCP(gameState);

            // ÉTAPE 4 : Envoyer l'état du drapeau
            sendFinishFlagStateToAll();

            System.out.println("[SERVEUR] État du jeu envoyé à joueur " + connection.getID());

        } catch (Exception e) {
            System.err.println("Erreur envoi état initial: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * REJOINDRE UN SERVEUR
     */
    public void connectToHost(String hostAddress) {
        System.out.println("Connexion à: " + hostAddress);

        // Si déjà connecté, déconnecter d'abord
        if (client != null && client.isConnected()) {
            System.out.println("Client déjà connecté, déconnexion...");
            client.close();
            try {
                Thread.sleep(100); // Petit délai pour nettoyer
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            // CRÉATION CLIENT
            client = new Client();

            // ENREGISTREMENT DES CLASSES
            registerClasses(client.getKryo());

            // ÉCOUTEUR CLIENT
            client.addListener(new Listener() {
                @Override
                public void connected(Connection connection) {
                    isConnected = true;
                    System.out.println("[CLIENT] Connecté au serveur");

                    // ENVOYER MESSAGE DE CONNEXION
                    sendJoinMessage();

                    if (networkListener != null) {
                        networkListener.onConnectedToServer();
                    }
                }

                @Override
                public void disconnected(Connection connection) {
                    isConnected = false;
                    System.out.println("[CLIENT] Déconnecté du serveur");

                    if (networkListener != null) {
                        networkListener.onDisconnectedFromServer();
                    }
                }

                @Override
                public void received(Connection connection, Object object) {
                    handleClientMessage(object);
                }
            });

            // DÉMARRAGE CLIENT
            client.start();
            System.out.println("Tentative de connexion TCP/UDP...");

            // CONNEXION
            client.connect(5000, hostAddress, TCP_PORT, UDP_PORT);

            System.out.println("Connexion établie avec le serveur!");

        } catch (IOException e) {
            System.err.println("Erreur connexion: " + e.getMessage());
            isConnected = false; // IMPORTANT
            if (networkListener != null) {
                networkListener.onConnectionFailed(e.getMessage());
            }
        }
    }

    /**
     * ENREGISTREMENT DES CLASSES AVEC KRYO
     *
     * Kryo a besoin de savoir quelles classes il peut sérialiser
     * On doit enregistrer chaque type de message qu'on utilise
     */
    private void registerClasses(com.esotericsoftware.kryo.Kryo kryo) {
        // CLASSES DE BASE
        kryo.register(NetworkMessage.class);

        // MESSAGES
        kryo.register(PlayerJoinMessage.class);
        kryo.register(PlayerInputMessage.class);
        kryo.register(GameStateMessage.class);
        kryo.register(GameStateMessage.PlayerData.class);
        kryo.register(PlayerLeaveMessage.class);
        kryo.register(PlayerCharacterMessage.class);

        // Enregistrer CollectibleStateMessage et ses classes internes
        kryo.register(CollectibleStateMessage.class);
        kryo.register(CollectibleStateMessage.CollectibleData.class);
        kryo.register(java.util.ArrayList.class); // Important pour la liste

        // Plateformes
        kryo.register(PlatformStateMessage.class);
        kryo.register(PlatformStateMessage.PlatformData.class);

        // Timer
        kryo.register(GameTimerMessage.class);

        // Drapeau de fin
        kryo.register(FinishFlagStateMessage.class);
        kryo.register(java.util.HashSet.class); // Pour le Set de joueurs

        // COLLECTIONS
        kryo.register(HashMap.class);
        kryo.register(java.util.ArrayList.class);

        // Spikes
        kryo.register(SpikeStateMessage.class);
        kryo.register(SpikeStateMessage.SpikeData.class);

        // Respawn
        kryo.register(PlayerRespawnMessage.class);
    }
    /**
     * ENVOYER MESSAGE DE CONNEXION
     */
    private void sendJoinMessage() {
        System.out.println("[CLIENT] Envoi de PlayerJoinMessage avec personnage : " + localPlayerCharacter);

        PlayerJoinMessage joinMessage = new PlayerJoinMessage(
            -1,
            localPlayerName,
            50, 300,
            localPlayerCharacter
        );

        client.sendTCP(joinMessage);
        System.out.println("[CLIENT] PlayerJoinMessage envoyé avec personnage : " + localPlayerCharacter);
    }

    /**
     * ENVOYER LES INPUTS DU JOUEUR LOCAL
     */
    public void sendPlayerInput(boolean leftPressed, boolean rightPressed,
                                boolean jumpPressed, int inputSequence) {
        if (client != null && client.isConnected()) {
            PlayerInputMessage inputMessage = new PlayerInputMessage(
                localPlayerId,
                leftPressed,
                rightPressed,
                jumpPressed,
                inputSequence
            );
            client.sendUDP(inputMessage);
        }
    }

    /**
     * GESTION DES MESSAGES COTÉ SERVEUR
     */
    private void handleServerMessage(Connection connection, Object object) {
        if (object instanceof PlayerJoinMessage) {
            handlePlayerJoin(connection, (PlayerJoinMessage) object);
        } else if (object instanceof PlayerInputMessage) {
            handlePlayerInput((PlayerInputMessage) object);
        } else if (object instanceof PlayerCharacterMessage) {
            handlePlayerCharacter((PlayerCharacterMessage) object);
        }
    }

    /**
     * GESTION D'UN NOUVEAU JOUEUR (Côté serveur)
     */
    private void handlePlayerJoin(Connection connection, PlayerJoinMessage message) {
        int newPlayerId = connection.getID();
        message.playerId = newPlayerId;

        GameStateMessage.PlayerData playerData = new GameStateMessage.PlayerData(
            message.startX, message.startY,
            0, 0, true, message.playerName
        );
        connectedPlayers.put(newPlayerId, playerData);

        ServerPlayer serverPlayer = new ServerPlayer(message.startX, message.startY, newPlayerId);
        serverPlayer.isGrounded = true;
        serverPlayers.put(newPlayerId, serverPlayer);

        // Utiliser le personnage du message
        String characterType = message.characterType != null ? message.characterType : "beige";
        playerCharacters.put(newPlayerId, characterType);

        System.out.println("[SERVEUR] Joueur " + newPlayerId + " rejoint avec personnage : " + characterType);

        // ✅ ÉTAPE 1 : Confirmation au joueur (TCP) - PRIORITAIRE
        connection.sendTCP(message);
        System.out.println("[SERVEUR] ✅ Confirmation envoyée à joueur " + newPlayerId);

        // ✅ NOUVEAU : Petit délai pour que la confirmation arrive AVANT les joueurs existants
        try {
            Thread.sleep(50); // 50ms de délai
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ÉTAPE 2 : Envoyer les joueurs existants AVEC leur personnage
        System.out.println("[SERVEUR] Envoi des " + (connectedPlayers.size() - 1) + " joueurs existants à " + newPlayerId);
        for (Map.Entry<Integer, GameStateMessage.PlayerData> entry : connectedPlayers.entrySet()) {
            if (entry.getKey() != newPlayerId) {
                // Récupérer le personnage du joueur existant
                String existingPlayerCharacter = playerCharacters.getOrDefault(entry.getKey(), "beige");

                PlayerJoinMessage existingPlayerMsg = new PlayerJoinMessage(
                    entry.getKey(),
                    entry.getValue().playerName,
                    entry.getValue().x,
                    entry.getValue().y,
                    existingPlayerCharacter
                );
                connection.sendTCP(existingPlayerMsg);

                // ✅ NOUVEAU : Petit délai entre chaque message
                try {
                    Thread.sleep(20); // 20ms entre chaque joueur
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("  → Envoi joueur existant ID: " + entry.getKey() + " (" + existingPlayerCharacter + ") à " + newPlayerId);
            }
        }

        // ÉTAPE 3 : Diffuser le nouveau joueur aux AUTRES (pas à lui-même)
        System.out.println("[SERVEUR] Broadcast du nouveau joueur " + newPlayerId + " aux autres");
        for (Connection conn : server.getConnections()) {
            if (conn.getID() != newPlayerId) {
                conn.sendTCP(message);
                System.out.println("  → Broadcast à joueur ID: " + conn.getID());
            }
        }

        // ÉTAPE 4 : Envoyer l'état initial (plateformes, collectibles)
        sendInitialGameStateToPlayer(connection);

        System.out.println("[SERVEUR] Nouveau joueur: " + message.playerName + " (ID: " + newPlayerId + ") - Total: " + connectedPlayers.size());
    }

    /**
     * GESTION DES INPUTS (Côté serveur)
     */
    private void handlePlayerInput(PlayerInputMessage message) {
        ServerPlayer serverPlayer = serverPlayers.get(message.playerId);
        if (serverPlayer == null) {
            return;
        }

        // Vérifier la séquence pour éviter les inputs en retard
        Integer lastSeq = lastReceivedSequence.get(message.playerId);

        if (lastSeq != null) {
            // Gérer le débordement de séquence
            int diff = message.inputSequence - lastSeq;

            // Si la différence est négative et grande, c'est un débordement
            if (diff < -500000) {
                // Débordement détecté (ex: 999999 -> 0)
                diff += Constants.MAX_SEQUENCE;
            }

            // Ignorer les inputs en retard (séquence plus ancienne)
            if (diff < 0) {
                System.out.println("Input en retard ignoré - Joueur: " + message.playerId +
                    ", Séquence reçue: " + message.inputSequence +
                    ", Dernière: " + lastSeq);
                return;
            }
        }

        // Mettre à jour la dernière séquence reçue
        lastReceivedSequence.put(message.playerId, message.inputSequence);

        // APPLIQUER LES INPUTS
        serverPlayer.currentInputX = 0;
        if (message.leftPressed) serverPlayer.currentInputX -= 1;
        if (message.rightPressed) serverPlayer.currentInputX += 1;
        serverPlayer.currentJumpPressed = message.jumpPressed;
    }

    /**
     * GÉRER LE CHOIX DE PERSONNAGE (CÔTÉ SERVEUR)
     */
    private void handlePlayerCharacter(PlayerCharacterMessage message) {
        System.out.println("[SERVEUR] Joueur " + message.playerId + " choisit : " + message.characterType);

        // Sauvegarder le choix
        playerCharacters.put(message.playerId, message.characterType);

        // Diffuser à tous les clients (y compris l'émetteur)
        if (server != null) {
            server.sendToAllTCP(message);
        }
    }

    /**
     * DIFFUSER L'ÉTAT DU JEU À TOUS LES CLIENTS - Version corrigée
     */
    private void sendGameStateToAll() {
        if (server == null) return;

        GameStateMessage gameState = new GameStateMessage();
        gameState.serverTime = System.currentTimeMillis();
        gameState.playerStates.putAll(connectedPlayers);

        // ENVOYER À TOUS LES CLIENTS CONNECTÉS
        server.sendToAllUDP(gameState);

        // LE HOST REÇOIT DIRECTEMENT LE MESSAGE
        if (isHost && networkListener != null) {
            networkListener.onGameStateReceived(gameState);
        }
    }

    /**
     * GESTION DÉCONNEXION JOUEUR (Côté serveur)
     */
    private void handlePlayerDisconnected(int playerId) {
        connectedPlayers.remove(playerId);
        serverPlayers.remove(playerId); // Nettoyer le joueur serveur
        lastReceivedSequence.remove(playerId); // Nettoyer la séquence

        // Informer tous les clients
        PlayerLeaveMessage leaveMessage = new PlayerLeaveMessage(playerId);
        server.sendToAllTCP(leaveMessage);

        // Mettre à jour l'état
        sendGameStateToAll();

        System.out.println("Joueur déconnecté: " + playerId);
    }

    /**
     * GESTION DES MESSAGES COTÉ CLIENT
     */
    private void handleClientMessage(Object object) {
        if (object instanceof PlayerJoinMessage) {
            PlayerJoinMessage message = (PlayerJoinMessage) object;

            // Vérifier si c'est NOTRE confirmation d'ID
            if (localPlayerId == -1) {
                // C'est forcément notre propre message de confirmation
                localPlayerId = message.playerId;
                System.out.println("[CLIENT] ID confirmé par le serveur: " + localPlayerId);

                // Notifier le listener
                if (networkListener != null) {
                    networkListener.onPlayerJoined(message);
                }
                return; // Ne pas créer de joueur pour nous-même
            }

            // Sinon, c'est un autre joueur qui rejoint
            System.out.println("[CLIENT] Autre joueur rejoint - ID: " + message.playerId);
            if (networkListener != null) {
                networkListener.onPlayerJoined(message);
            }
        } else if (object instanceof GameStateMessage) {
            if (networkListener != null) {
                networkListener.onGameStateReceived((GameStateMessage) object);
            }

        } else if (object instanceof PlayerLeaveMessage) {
            if (networkListener != null) {
                networkListener.onPlayerLeft((PlayerLeaveMessage) object);
            }
        } else if (object instanceof PlatformStateMessage) {
            // Gérer les plateformes
            if (networkListener != null) {
                networkListener.onPlatformStateReceived((PlatformStateMessage) object);
            }
        }else if (object instanceof CollectibleStateMessage) {
            // Gérer les collectible
            if (networkListener != null) {
                networkListener.onCollectibleStateReceived((CollectibleStateMessage) object);
            }
        } else if (object instanceof GameTimerMessage) {
            // Gérer le timer
            if (networkListener != null) {
                networkListener.onGameTimerReceived((GameTimerMessage) object);
            }
        } else if (object instanceof FinishFlagStateMessage) {
            // Gérer l'état du drapeau
            if (networkListener != null) {
                networkListener.onFinishFlagStateReceived((FinishFlagStateMessage) object);
            }
        } else if (object instanceof SpikeStateMessage) {
            // Gérer les spikes
            if (networkListener != null) {
                networkListener.onSpikeStateReceived((SpikeStateMessage) object);
            }
        } else if (object instanceof PlayerRespawnMessage) {
            // Gérer le respawn
            if (networkListener != null) {
                networkListener.onPlayerRespawned((PlayerRespawnMessage) object);
            }
        } else if (object instanceof PlayerCharacterMessage) {
            if (networkListener != null) {
                networkListener.onPlayerCharacterChanged((PlayerCharacterMessage) object);
            }
        }
    }

    // GETTERS & SETTERS

    public void setNetworkListener(NetworkListener listener) {
        this.networkListener = listener;
    }

    public void setLocalPlayerName(String name) {
        this.localPlayerName = name;
    }

    public int getLocalPlayerId() {
        return localPlayerId;
    }

    public boolean isHost() {
        return isHost;
    }

    public boolean isConnected() {
        return isConnected;
    }

    /**
     * OBTENIR LE TYPE DE PERSONNAGE D'UN JOUEUR
     */
    public String getPlayerCharacter(int playerId) {
        return playerCharacters.getOrDefault(playerId, "beige");
    }

    /**
     * FERMER LES CONNEXIONS
     */
    public void disconnect() {
        System.out.println("DISCONNECT appelé - Host: " + isHost + ", Connecté: " + isConnected);

        if (server != null) {
            System.out.println("Fermeture du serveur...");
            server.stop();
        }
        if (client != null) {
            System.out.println("Fermeture du client...");
            client.close();
        }
        isConnected = false;
    }
}
