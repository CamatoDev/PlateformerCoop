package com.vortexmakers.plateformer.network.messages;

import java.util.HashMap;
import java.util.Map;

/**
 * MESSAGE D'ÉTAT DU LOBBY
 * Envoyé par le serveur pour synchroniser la liste des joueurs dans le lobby
 */
public class LobbyStateMessage extends NetworkMessage {
    public Map<Integer, LobbyPlayerData> players;

    public LobbyStateMessage() {
        this.players = new HashMap<>();
    }

    /**
     * DONNÉES D'UN JOUEUR DANS LE LOBBY
     */
    public static class LobbyPlayerData {
        public int playerId;
        public String playerName;
        public String characterType;
        public boolean isHost;

        public LobbyPlayerData() {
            // Constructeur vide pour Kryo
        }

        public LobbyPlayerData(int playerId, String playerName, String characterType, boolean isHost) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.characterType = characterType;
            this.isHost = isHost;
        }
    }
}
