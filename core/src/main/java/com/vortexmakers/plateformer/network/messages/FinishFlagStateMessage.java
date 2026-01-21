package com.vortexmakers.plateformer.network.messages;

import java.util.HashSet;
import java.util.Set;

/**
 * MESSAGE POUR SYNCHRONISER L'ÉTAT DU DRAPEAU
 */
public class FinishFlagStateMessage extends NetworkMessage {
    public float flagX, flagY;                    // Position du drapeau
    public Set<Integer> playersWhoFinished;       // IDs des joueurs ayant atteint le drapeau
    public boolean allPlayersFinished;            // Tous les joueurs ont-ils fini ?

    public FinishFlagStateMessage() {
        this.playersWhoFinished = new HashSet<>();
    }

    public FinishFlagStateMessage(float flagX, float flagY, Set<Integer> playersWhoFinished, boolean allPlayersFinished) {
        this.flagX = flagX;
        this.flagY = flagY;
        this.playersWhoFinished = new HashSet<>(playersWhoFinished);
        this.allPlayersFinished = allPlayersFinished;
    }
}
