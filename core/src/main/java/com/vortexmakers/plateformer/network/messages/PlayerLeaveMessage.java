package com.vortexmakers.plateformer.network.messages;

// MESSAGE DE DÉCONNEXION - Quand un joueur quitte la partie
public class PlayerLeaveMessage extends NetworkMessage {
    public PlayerLeaveMessage() {
    }

    public PlayerLeaveMessage(int playerId) {
        super(playerId);
    }
}
