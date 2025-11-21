package com.vortexmakers.plateformer.network;

// IMPORTATIONS KRYONET ==================================
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

// IMPORTATIONS MESSAGES =================================
import com.vortexmakers.plateformer.network.listeners.NetworkListener;
import com.vortexmakers.plateformer.network.messages.*;

// IMPORTATIONS JAVA =====================================
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * NETWORKMANAGER - Gestionnaire réseau principal
 *
 * Peut fonctionner en mode :
 * - SERVEUR (Host) : Héberge la partie, fait autorité
 * - CLIENT : Se connecte à un serveur
 */
public class NetworkManager {
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

    // Pour le serveur : suivre tous les joueurs
    private Map<Integer, GameStateMessage.PlayerData> connectedPlayers;

    public NetworkManager() {
        this.connectedPlayers = new HashMap<>();
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

        } catch (IOException e) {
            System.err.println("❌ Erreur démarrage serveur: " + e.getMessage());
            if (networkListener != null) {
                networkListener.onConnectionFailed(e.getMessage());
            }
        }
    }

    /**
     * REJOINDRE UN SERVEUR
     */
    public void connectToHost(String hostAddress) {
        System.out.println("🔗 Connexion à: " + hostAddress);

        isHost = false;

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
                        System.out.println("🔌 [CLIENT] Déconnecté du serveur");
                        networkListener.onConnectedToServer();
                    }else {
                        System.out.println("❌ [CLIENT] networkListener est NULL!");
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
            // ✅ AJOUT: Log avant connexion
            System.out.println("🔗 Tentative de connexion TCP/UDP...");

            // CONNEXION ================================
            client.connect(5000, hostAddress, TCP_PORT, UDP_PORT);

            // ✅ AJOUT: Log après connexion réussie
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

        // COLLECTIONS
        kryo.register(HashMap.class);
        kryo.register(java.util.ArrayList.class);
    }

    /**
     * ENVOYER MESSAGE DE CONNEXION
     */
    private void sendJoinMessage() {
        System.out.println("📤 [CLIENT] Envoi de PlayerJoinMessage...");

        PlayerJoinMessage joinMessage = new PlayerJoinMessage(
            localPlayerId,
            localPlayerName,
            100, 300  // Position de départ
        );
        System.out.println("📤 [CLIENT] PlayerJoinMessage créé - ID: " + joinMessage.playerId);
        client.sendTCP(joinMessage);
        System.out.println("📤 [CLIENT] PlayerJoinMessage envoyé via TCP");
    }

    /**
     * ENVOYER LES INPUTS DU JOUEUR LOCAL
     */
    public void sendPlayerInput(float velocityX, boolean jumpPressed,
                                boolean movingLeft, boolean movingRight,
                                float currentX, float currentY) {
        if (client != null && client.isConnected()) {
            PlayerInputMessage inputMessage = new PlayerInputMessage(
                localPlayerId,
                velocityX,
                jumpPressed,
                movingLeft,
                movingRight,
                currentX,
                currentY
            );
            client.sendUDP(inputMessage); // UDP pour la rapidité
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

        // Assigner un ID au joueur
        message.playerId = newPlayerId;
        localPlayerId = newPlayerId;

        // Stocker le joueur
        GameStateMessage.PlayerData playerData = new GameStateMessage.PlayerData(
            message.startX, message.startY,
            0, 0, false, message.playerName
        );
        connectedPlayers.put(newPlayerId, playerData);

        // Diffuser à tous
        server.sendToAllTCP(message);

        // Envoyer l'état actuel au nouveau joueur
        sendGameStateToAll();

        System.out.println("🎮 Nouveau joueur: " + message.playerName + " (ID: " + newPlayerId + ")");
    }

    /**
     * GESTION DES INPUTS (Côté serveur)
     */
    private void handlePlayerInput(PlayerInputMessage message) {
        // Mettre à jour le joueur
        GameStateMessage.PlayerData playerData = connectedPlayers.get(message.playerId);
        if (playerData != null) {
            playerData.x = message.currentX;
            playerData.y = message.currentY;
            playerData.velocityX = message.velocityX;

            // Ici, on ferait une vraie simulation physique
            // Pour l'instant, on fait confiance au client pour la position
        }

        // Diffuser l'état mis à jour
        sendGameStateToAll();
    }

    /**
     * DIFFUSER L'ÉTAT DU JEU À TOUS LES CLIENTS
     */
    private void sendGameStateToAll() {
        GameStateMessage gameState = new GameStateMessage();
        gameState.serverTime = System.currentTimeMillis();
        gameState.playerStates.putAll(connectedPlayers);

        server.sendToAllUDP(gameState); // UDP pour la rapidité
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

            // Si c'est notre propre message de connexion
            if (message.playerId == localPlayerId) {
                localPlayerId = message.playerId;
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
