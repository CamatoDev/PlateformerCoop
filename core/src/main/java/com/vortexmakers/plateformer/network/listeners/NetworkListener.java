package com.vortexmakers.plateformer.network.listeners;


import com.vortexmakers.plateformer.network.messages.GameStateMessage;
import com.vortexmakers.plateformer.network.messages.PlayerJoinMessage;
import com.vortexmakers.plateformer.network.messages.PlayerLeaveMessage;

/**
 * INTERFACE POUR ÉCOUTER LES ÉVÉNEMENTS RÉSEAU
 *
 * Les écrans (LobbyScreen, GameScreen) implémentent cette interface
 * pour recevoir les messages réseau.
 */
public interface NetworkListener {
    void onPlayerJoined(PlayerJoinMessage message);
    void onPlayerLeft(PlayerLeaveMessage message);
    void onGameStateReceived(GameStateMessage message);
    void onConnectedToServer();
    void onDisconnectedFromServer();
    void onConnectionFailed(String reason);
}
