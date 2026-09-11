package com.vortexmakers.plateformer.network.messages;

/**
 * MESSAGE DE LANCEMENT DE PARTIE
 * Envoyé par le serveur quand tous les joueurs sont prêts
 */
public class GameStartMessage extends NetworkMessage {

    public GameStartMessage() {
        // Constructeur vide requis par Kryo
    }
}
