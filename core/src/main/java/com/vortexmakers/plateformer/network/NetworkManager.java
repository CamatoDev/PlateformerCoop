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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

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

    // CONFIGURATION RÉSEAU ==============================
    private static final int TCP_PORT = 54555;   // Port TCP pour données fiables
    private static final int UDP_PORT = 54777;   // Port UDP pour données rapides

    // RÉFÉRENCES RÉSEAU ================================
    private Server server;
    private Client client;

    // ÉTATS ============================================
    private boolean isHost = false;
    private boolean isConnected = false;
    private NetworkListener networkListener;

    // DONNÉES JOUEURS ==================================
    private int localPlayerId = -1;  // Notre ID (assigné par le serveur)
    private String localPlayerName = "Player";

    // POUR LE SERVEUR AUTORITAIRE ======================
    private Map<Integer, ServerPlayer> serverPlayers;
    private List<CollectibleStateMessage.CollectibleData> serverCollectibles;
    // ✅ MODIFICATION: Utiliser PlatformData au lieu de Rectangle
    private List<PlatformStateMessage.PlatformData> serverPlatforms;
    private int nextCollectibleId = 0;

    // TIMING SERVEUR ===================================
    private float serverUpdateTimer = 0f;
    private static final float SERVER_UPDATE_INTERVAL = 1f / 20f; // 60Hz

    // Pour le serveur : suivre tous les joueurs
    private Map<Integer, GameStateMessage.PlayerData> connectedPlayers;

    // CONSTRUCTEUR PRIVÉ
    private NetworkManager() {
        this.connectedPlayers = new HashMap<>();
        this.serverPlayers = new HashMap<>();
        this.serverCollectibles = new ArrayList<>();
        this.serverPlatforms = new ArrayList<>();
        System.out.println("🎮 NetworkManager créé (Singleton)");
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
     * DÉMARRER EN MODE HOST (Serveur SEULEMENT)
     */
    public void startHost() {
        System.out.println("🚀 Démarrage du serveur...");

        isHost = true;

        try {
            // CRÉATION SERVEUR ==========================
            server = new Server();

            // ENREGISTREMENT DES CLASSES ================
            registerClasses(server.getKryo());

            // DÉMARRAGE SERVEUR ========================
            server.start();
            server.bind(TCP_PORT, UDP_PORT);

            // ÉCOUTEUR SERVEUR =========================
            server.addListener(new Listener() {
                @Override
                public void connected(Connection connection) {
                    System.out.println("📞 Client connecté: " + connection.getID() + " from " + connection.getRemoteAddressTCP());
                }

                @Override
                public void disconnected(Connection connection) {
                    System.out.println("📞 Client déconnecté: " + connection.getID());
                    handlePlayerDisconnected(connection.getID());
                }

                @Override
                public void received(Connection connection, Object object) {
                    handleServerMessage(connection, object);
                }
            });

            System.out.println("✅ Serveur démarré sur port " + TCP_PORT + " - En attente de joueurs...");

            // ✅ IMPORTANT: Le host doit maintenant se connecter en client
            // Mais on laisse le LobbyScreen gérer cette connexion
            System.out.println("🎮 Host - Prêt à se connecter en client");

            // INITIALISATION DU MONDE SERVEUR ==========
            initializeServerWorld();

            System.out.println("✅ Serveur autoritaire démarré");

        } catch (IOException e) {
            System.err.println("❌ Erreur démarrage serveur: " + e.getMessage());
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

        // ✅ AJOUT: Envoyer l'état des plateformes
        sendPlatformStateToAll();
        sendCollectibleStateToAll();

        // ✅ CORRECTION: Envoyer immédiatement l'état des collectibles
        sendCollectibleStateToAll();

        System.out.println("🎯 Monde serveur initialisé avec " + serverCollectibles.size() + " collectibles et " + serverPlatforms.size() + " plateformes");
    }

    /**
     * CRÉATION DES PLATEFORMES SUR LE SERVEUR
     */
    private void createServerPlatforms() {
        serverPlatforms.clear(); // ✅ S'assurer que c'est vide

        // ✅ UTILISER PlatformData DIRECTEMENT
        // Plateforme de base (sol)
        for (int i = 0; i < 15; i++) {
            serverPlatforms.add(new PlatformStateMessage.PlatformData(
                i * 200, 0, 200, Constants.PLATFORM_HEIGHT, 0 // type 0 = terrain
            ));
        }

        // Quelques plateformes de test
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

        System.out.println("🏗️ " + serverPlatforms.size() + " plateformes créées sur le serveur");
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
        if (serverUpdateTimer >= SERVER_UPDATE_INTERVAL) {
            serverUpdateTimer = 0f;

            // ✅ CORRECTION: Toujours mettre à jour et envoyer, même sans joueurs
            if (!serverPlayers.isEmpty()) {
                // APPLIQUER LA PHYSIQUE À TOUS LES JOUEURS
                for (ServerPlayer serverPlayer : serverPlayers.values()) {
                    serverPlayer.applyServerPhysics(SERVER_UPDATE_INTERVAL);
                    checkServerPlatformCollisions(serverPlayer);
                    checkServerCollectibleCollisions(serverPlayer);
                }

                updateConnectedPlayersFromServer();
            }

            // ✅ CORRECTION: Envoyer les états même sans joueurs (pour synchronisation initiale)
            sendGameStateToAll();
            sendPlatformStateToAll();
            sendCollectibleStateToAll();
        }
    }

    /**
     * RÉSOLUTION DES COLLISIONS (côté serveur) - Version corrigée
     */
    private boolean resolveServerCollision(ServerPlayer serverPlayer, Rectangle platform) {
        Rectangle playerBounds = serverPlayer.getBounds();

        // ✅ CORRECTION: Calcul correct des chevauchements
        float overlapLeft = playerBounds.x + playerBounds.width - platform.x;
        float overlapRight = platform.x + platform.width - playerBounds.x;
        float overlapTop = platform.y + platform.height - playerBounds.y; // CORRIGÉ
        float overlapBottom = playerBounds.y + playerBounds.height - platform.y; // CORRIGÉ

        // Vérifier que les chevauchements sont positifs
        if (overlapLeft <= 0 || overlapRight <= 0 || overlapTop <= 0 || overlapBottom <= 0) {
            return false;
        }

        // TROUVER LA PLUS PETITE PÉNÉTRATION
        float minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
            Math.min(overlapTop, overlapBottom));

        // RÉSOLUTION DE LA COLLISION
        if (minOverlap == overlapTop) {
            // COLLISION PAR LE HAUT : le joueur atterrit sur la plateforme
            serverPlayer.y = platform.y + platform.height;
            serverPlayer.velocityY = 0;
            return true;

        } else if (minOverlap == overlapBottom) {
            // COLLISION PAR LE BAS : le joueur heurte le plafond
            serverPlayer.y = platform.y - playerBounds.height;
            serverPlayer.velocityY = 0;
            return false;

        } else if (minOverlap == overlapLeft) {
            // COLLISION PAR LA GAUCHE
            serverPlayer.x = platform.x - playerBounds.width;
            serverPlayer.velocityX = 0;
            return false;

        } else if (minOverlap == overlapRight) {
            // COLLISION PAR LA DROITE
            serverPlayer.x = platform.x + platform.width;
            serverPlayer.velocityX = 0;
            return false;
        }

        return false;
    }

    /**
     * VÉRIFICATION DES COLLISIONS PLATEFORMES (côté serveur)
     */
    private void checkServerPlatformCollisions(ServerPlayer serverPlayer) {
        boolean grounded = false;
        int collisionCount = 0;

        for (PlatformStateMessage.PlatformData platform : serverPlatforms) {
            // ✅ CONVERTIR PlatformData en Rectangle pour les collisions
            Rectangle platformRect = new Rectangle(platform.x, platform.y, platform.width, platform.height);

            if (serverPlayer.getBounds().overlaps(platformRect)) {
                collisionCount++;
                grounded = resolveServerCollision(serverPlayer, platformRect) || grounded;
            }
        }

        if (collisionCount > 0) {
            //System.out.println("🔄 " + collisionCount + " collision(s) résolue(s) pour joueur " + serverPlayer.playerId);
        }

        serverPlayer.isGrounded = grounded;

        if (serverPlayer.isGrounded) {
            System.out.println("🟢 Joueur " + serverPlayer.playerId + " est au sol");
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
                    // ✅ MARQUER COMME COLLECTÉ IMMÉDIATEMENT
                    collectible.collected = true;
                    collectible.collectedByPlayerId = serverPlayer.playerId;

                    System.out.println("🎯 Serveur: Collectible " + collectible.collectibleId + " collecté");

                    // ✅ ENVOYER IMMÉDIATEMENT l'état mis à jour
                    sendCollectibleStateToAll();
                    break; // Un collectible par frame
                }
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

            // ✅ SYNCHRONISATION COMPLÈTE
            playerData.x = serverPlayer.x;
            playerData.y = serverPlayer.y;
            playerData.velocityX = serverPlayer.velocityX;
            playerData.velocityY = serverPlayer.velocityY;
            playerData.isGrounded = serverPlayer.isGrounded;
            playerData.playerName = "Player " + playerId;
        }
    }

    /**
     * ENVOYER L'ÉTAT DES PLATEFORMES À TOUS LES CLIENTS
     */
    private void sendPlatformStateToAll() {
        try {
            PlatformStateMessage platformState = new PlatformStateMessage();
            platformState.platforms.addAll(serverPlatforms); // ✅ SIMPLE COPIE

            if (server != null && server.getConnections().length > 0) {
                server.sendToAllTCP(platformState);
                //System.out.println("🏗️ État plateformes envoyé à " + server.getConnections().length + " clients");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi état plateformes: " + e.getMessage());
        }
    }

    /**
     * ENVOYER L'ÉTAT DES COLLECTIBLES À TOUS LES CLIENTS
     */
    private void sendCollectibleStateToAll() {
        try {
            CollectibleStateMessage collectibleState = new CollectibleStateMessage();
            collectibleState.collectibles.addAll(serverCollectibles);

            // ✅ AJOUT: Vérification avant envoi
            if (server != null && server.getConnections().length > 0) {
                server.sendToAllUDP(collectibleState);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi état collectibles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ MODIFICATION: sendInitialGameState pour tout envoyer
    public void sendInitialGameState() {
        System.out.println("📦 Envoi de l'état initial complet");
        sendPlatformStateToAll();
        sendGameStateToAll();
        sendCollectibleStateToAll();
    }

    /**
     * REJOINDRE UN SERVEUR
     */
    public void connectToHost(String hostAddress) {
        System.out.println("🔗 Connexion à: " + hostAddress);

        // ✅ CORRECTION: Ne pas changer le statut host quand on se connecte
        // isHost = false;  // SUPPRIMER CETTE LIGNE

        try {
            // CRÉATION CLIENT ===========================
            client = new Client();

            // ENREGISTREMENT DES CLASSES ================
            registerClasses(client.getKryo());

            // ÉCOUTEUR CLIENT ==========================
            client.addListener(new Listener() {
                @Override
                public void connected(Connection connection) {
                    isConnected = true;
                    System.out.println("✅ [CLIENT] Connecté au serveur");

                    // ENVOYER MESSAGE DE CONNEXION =====
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

            // DÉMARRAGE CLIENT =========================
            client.start();
            System.out.println("🔗 Tentative de connexion TCP/UDP...");

            // CONNEXION ================================
            client.connect(5000, hostAddress, TCP_PORT, UDP_PORT);

            System.out.println("✅ Connexion établie avec le serveur!");

        } catch (IOException e) {
            System.err.println("Erreur connexion: " + e.getMessage());
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

        // ✅ AJOUT: Enregistrer CollectibleStateMessage et ses classes internes
        kryo.register(CollectibleStateMessage.class);
        kryo.register(CollectibleStateMessage.CollectibleData.class);
        kryo.register(java.util.ArrayList.class); // Important pour la liste

        // ✅ AJOUT: Plateformes
        kryo.register(PlatformStateMessage.class);
        kryo.register(PlatformStateMessage.PlatformData.class);

        // COLLECTIONS
        kryo.register(HashMap.class);
        kryo.register(java.util.ArrayList.class);
    }
    /**
     * ENVOYER MESSAGE DE CONNEXION
     */
    private void sendJoinMessage() {
        System.out.println("📤 [CLIENT] Envoi de PlayerJoinMessage...");

        // ✅ CORRECTION: Ne pas utiliser localPlayerId (-1) dans le message
        // Le serveur assignera l'ID correct
        PlayerJoinMessage joinMessage = new PlayerJoinMessage(
            -1, // Le serveur remplacera par le vrai ID
            localPlayerName,
            50, 300  // Position de départ
        );
        System.out.println("📤 [CLIENT] PlayerJoinMessage créé - Nom: " + joinMessage.playerName);
        client.sendTCP(joinMessage);
        System.out.println("📤 [CLIENT] PlayerJoinMessage envoyé via TCP");
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
        }
    }

    /**
     * GESTION D'UN NOUVEAU JOUEUR (Côté serveur)
     */
    private void handlePlayerJoin(Connection connection, PlayerJoinMessage message) {
        int newPlayerId = connection.getID();
        message.playerId = newPlayerId;

        // ✅ CORRECTION: Position de départ SUR le sol
        float startY = 300; // Début sur la première plateforme
        GameStateMessage.PlayerData playerData = new GameStateMessage.PlayerData(
            message.startX, message.startY, // Y corrigé
            0, 0, false, message.playerName // Déjà grounded
        );
        connectedPlayers.put(newPlayerId, playerData);

        // ✅ CRÉER LE JOUEUR SERVEUR avec la même position
        ServerPlayer serverPlayer = new ServerPlayer(message.startX, message.startY, newPlayerId);
        serverPlayer.isGrounded = true; // Déjà sur le sol
        serverPlayers.put(newPlayerId, serverPlayer);

        // Diffuser à tous
        server.sendToAllTCP(message);

        // Envoyer l'état actuel au nouveau joueur
        sendInitialGameState();

        System.out.println("🎮 Nouveau joueur: " + message.playerName + " (ID: " + newPlayerId + ")");
    }

    /**
     * GESTION DES INPUTS (Côté serveur)
     */
    private void handlePlayerInput(PlayerInputMessage message) {
        ServerPlayer serverPlayer = serverPlayers.get(message.playerId);
        if (serverPlayer != null) {
            // METTRE À JOUR LES INPUTS DU JOUEUR SERVEUR
            serverPlayer.currentInputX = 0;
            if (message.leftPressed) serverPlayer.currentInputX -= 1;
            if (message.rightPressed) serverPlayer.currentInputX += 1;
            serverPlayer.currentJumpPressed = message.jumpPressed;
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

        // Informer tous les clients
        PlayerLeaveMessage leaveMessage = new PlayerLeaveMessage(playerId);
        server.sendToAllTCP(leaveMessage);

        // Mettre à jour l'état
        sendGameStateToAll();

        System.out.println("👋 Joueur déconnecté: " + playerId);
    }

    /**
     * GESTION DES MESSAGES COTÉ CLIENT
     */
    private void handleClientMessage(Object object) {

        if (object instanceof PlayerJoinMessage) {
            PlayerJoinMessage message = (PlayerJoinMessage) object;
            System.out.println("🆔 [CLIENT] PlayerJoinMessage reçu - ID: " + message.playerId + ", Nom: " + message.playerName);

            // ✅ CORRECTION: Toujours mettre à jour notre ID si on reçoit un PlayerJoinMessage nous concernant
            if (message.playerName.equals(localPlayerName) || message.playerId == localPlayerId) {
                localPlayerId = message.playerId;
                System.out.println("🆔 [CLIENT] ID mis à jour: " + localPlayerId);
            }

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
            // ✅ AJOUT: Gérer les plateformes
            if (networkListener != null) {
                networkListener.onPlatformStateReceived((PlatformStateMessage) object);
            }
        }else if (object instanceof CollectibleStateMessage) {
            // ✅ AJOUTER CE CAS :
            if (networkListener != null) {
                networkListener.onCollectibleStateReceived((CollectibleStateMessage) object);
            }
        }
    }

    // GETTERS & SETTERS ================================

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
     * FERMER LES CONNEXIONS
     */
    public void disconnect() {
        System.out.println("🔌 DISCONNECT appelé - Host: " + isHost + ", Connecté: " + isConnected);

        if (server != null) {
            System.out.println("🔌 Fermeture du serveur...");
            server.stop();
        }
        if (client != null) {
            System.out.println("🔌 Fermeture du client...");
            client.close();
        }
        isConnected = false;
    }
}
