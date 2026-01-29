package com.vortexmakers.plateformer.network.messages;

public class PlayerJoinMessage extends NetworkMessage {
    // Nom du joueur (pour l'affichage)
    public String playerName;

    // Position initiale du joueur
    public float startX, startY;

    // ✅ NOUVEAU : Type de personnage
    public String characterType;

    public PlayerJoinMessage() {
        // Constructeur vide requis par Kryo
    }

    public PlayerJoinMessage(int playerId, String playerName, float startX, float startY) {
        super(playerId);
        this.playerName = playerName;
        this.startX = startX;
        this.startY = startY;
        this.characterType = "beige"; // Valeur par défaut
    }

    // Constructeur avec personnage
    public PlayerJoinMessage(int playerId, String playerName, float startX, float startY, String characterType) {
        super(playerId);
        this.playerName = playerName;
        this.startX = startX;
        this.startY = startY;
        this.characterType = characterType;
    }
}
