package com.vortexmakers.plateformer.network.listeners;


import com.vortexmakers.plateformer.network.messages.*;
/**
 * INTERFACE POUR ÉCOUTER LES ÉVÉNEMENTS RÉSEAU
 *
 * Les écrans (LobbyScreen, GameScreen) implémentent cette interface
 * pour recevoir les messages réseau.
 */
public interface NetworkListener {
    void onPlayerJoined(PlayerJoinMessage message);
    void onPlayerCharacterChanged(PlayerCharacterMessage message);
    void onLobbyStateReceived(LobbyStateMessage message);
    void onPlayerLeft(PlayerLeaveMessage message);
    void onGameStateReceived(GameStateMessage message);
    void onPlatformStateReceived(PlatformStateMessage message);
    void onCollectibleStateReceived(CollectibleStateMessage message);
    void onGameTimerReceived(GameTimerMessage message);
    void onSpikeStateReceived(SpikeStateMessage message);
    void onPlayerRespawned(PlayerRespawnMessage message);
    void onFinishFlagStateReceived(FinishFlagStateMessage message);
    void onConnectedToServer();
    void onDisconnectedFromServer();
    void onConnectionFailed(String reason);
    void onPlayerReadyStateReceived(LobbyStateMessage message);
    void onGameStartReceived();
}
