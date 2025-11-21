package com.vortexmakers.plateformer.network.messages;


public class PlayerJoinMessage extends NetworkMessage {
    // Nom du joueur (pour l'affichage)
    public String playerName;

    // Position initiale du joueur
    public float startX, startY;

    public PlayerJoinMessage() {
        // Constructeur vide requis par Kryo
    }

    public PlayerJoinMessage(int playerId, String playerName, float startX, float startY) {
        super(playerId);
        this.playerName = playerName;
        this.startX = startX;
        this.startY = startY;
    }
}
