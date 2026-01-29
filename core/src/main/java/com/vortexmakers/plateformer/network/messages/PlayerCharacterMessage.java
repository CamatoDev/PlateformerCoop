package com.vortexmakers.plateformer.network.messages;

/**
 * MESSAGE DE SÉLECTION DE PERSONNAGE
 * Envoyé quand un joueur choisit son personnage dans le lobby
 */
public class PlayerCharacterMessage extends NetworkMessage {
    public String characterType; // "beige", "green", "pink", "purple", "yellow"

    public PlayerCharacterMessage() {
        // Constructeur vide requis par Kryo
    }

    public PlayerCharacterMessage(int playerId, String characterType) {
        super(playerId);
        this.characterType = characterType;
    }
}
