package com.vortexmakers.plateformer.network.messages;


import java.io.Serializable;

//CLASSE DE BASE POUR TOUS LES MESSAGES RÉSEAU
public abstract class NetworkMessage implements Serializable {
    // Timestamp pour suivre quand le message a été créé
    public long timestamp;

    // ID du joueur qui envoie le message
    public int playerId;

    public NetworkMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public NetworkMessage(int playerId) {
        this();
        this.playerId = playerId;
    }
}
