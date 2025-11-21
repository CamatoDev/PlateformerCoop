package com.vortexmakers.plateformer.network.messages;


import java.util.HashMap;
import java.util.Map;

public class GameStateMessage extends NetworkMessage {
    // Map : PlayerID → Données du joueur
    public Map<Integer, PlayerData> playerStates;

    // Temps serveur pour synchronisation
    public long serverTime;

    public GameStateMessage() {
        this.playerStates = new HashMap<>();
    }

    /**
     * DONNÉES D'UN JOUEUR - Position, vitesse, état
     */
    public static class PlayerData {
        public float x, y;
        public float velocityX, velocityY;
        public boolean isGrounded;
        public String playerName;

        public PlayerData() {
            // Constructeur vide requis par Kryo
        }

        public PlayerData(float x, float y, float velocityX, float velocityY,
                          boolean isGrounded, String playerName) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.isGrounded = isGrounded;
            this.playerName = playerName;
        }
    }
}
