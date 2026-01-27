package com.vortexmakers.plateformer.network.messages;

/**
 * MESSAGE DE RESPAWN - Informe tous les clients qu'un joueur respawn
 *
 * Envoyé par le serveur quand un joueur :
 * - Tombe dans le vide (Y < seuil)
 * - Touche un spike
 * - (Futur : touche un ennemi)
 */
public class PlayerRespawnMessage extends NetworkMessage {
    public float spawnX;  // Position de respawn X
    public float spawnY;  // Position de respawn Y
    public boolean invincible; // 2 secondes d'invincibilité après respawn

    public PlayerRespawnMessage() {
        // Constructeur vide requis par Kryo
    }

    public PlayerRespawnMessage(int playerId, float spawnX, float spawnY, boolean invincible) {
        super(playerId);
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.invincible = invincible;
    }
}
