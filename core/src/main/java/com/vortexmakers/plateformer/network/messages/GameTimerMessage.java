package com.vortexmakers.plateformer.network.messages;

/**
 * MESSAGE POUR SYNCHRONISER LE TIMER DU JEU
 */
public class GameTimerMessage extends NetworkMessage {
    public float currentTime;      // Temps écoulé depuis le début
    public float timeLimit;        // Limite de temps du niveau
    public boolean timerStarted;   // Le timer est-il démarré ?

    public GameTimerMessage() {
        // Constructeur vide requis par Kryo
    }

    public GameTimerMessage(float currentTime, float timeLimit, boolean timerStarted) {
        this.currentTime = currentTime;
        this.timeLimit = timeLimit;
        this.timerStarted = timerStarted;
    }
}
