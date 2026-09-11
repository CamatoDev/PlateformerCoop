package com.vortexmakers.plateformer.network.messages;

/**
 * MESSAGE DE STATUT PRÊT
 * Envoyé par un client quand il clique sur le bouton "Prêt" / "Pas prêt"
 */
public class PlayerReadyMessage extends NetworkMessage {
    public boolean isReady;

    public PlayerReadyMessage() {
        // Constructeur vide requis par Kryo
    }

    public PlayerReadyMessage(int playerId, boolean isReady) {
        super(playerId);
        this.isReady = isReady;
    }
}
