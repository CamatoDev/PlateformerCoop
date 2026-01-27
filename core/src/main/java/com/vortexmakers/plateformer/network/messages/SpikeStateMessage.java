package com.vortexmakers.plateformer.network.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * MESSAGE POUR SYNCHRONISER LES SPIKES
 * Envoyé une fois au début du jeu pour que tous les clients
 * créent les mêmes spikes aux mêmes positions
 */
public class SpikeStateMessage extends NetworkMessage {
    public List<SpikeData> spikes;

    public SpikeStateMessage() {
        this.spikes = new ArrayList<>();
    }

    /**
     * DONNÉES D'UN SPIKE
     */
    public static class SpikeData {
        public float x, y;

        public SpikeData() {
            // Constructeur vide pour Kryo
        }

        public SpikeData(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
