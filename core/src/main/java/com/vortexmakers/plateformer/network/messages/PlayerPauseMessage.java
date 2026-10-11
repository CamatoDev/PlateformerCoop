package com.vortexmakers.plateformer.network.messages;

/**
 * MESSAGE DE PAUSE - Un joueur ouvre / ferme son menu pause.
 * Le serveur le relaie à tous les clients pour afficher l'indicateur "En pause".
 */
public class PlayerPauseMessage extends NetworkMessage {
    public boolean paused;

    public PlayerPauseMessage() {
    }

    public PlayerPauseMessage(int playerId, boolean paused) {
        super(playerId);
        this.paused = paused;
    }
}
