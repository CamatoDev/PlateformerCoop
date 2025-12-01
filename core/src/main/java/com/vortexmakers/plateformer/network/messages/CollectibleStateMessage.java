package com.vortexmakers.plateformer.network.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * MESSAGE POUR SYNCHRONISER LES COLLECTIBLES
 */
public class CollectibleStateMessage extends NetworkMessage {
    public List<CollectibleData> collectibles;

    public CollectibleStateMessage() {
        this.collectibles = new ArrayList<>();
    }

    public static class CollectibleData {
        public int collectibleId;
        public float x, y;
        public boolean collected;
        public int collectedByPlayerId; // -1 si pas collecté

        public CollectibleData() {
        }

        public CollectibleData(int collectibleId, float x, float y, boolean collected, int collectedByPlayerId) {
            this.collectibleId = collectibleId;
            this.x = x;
            this.y = y;
            this.collected = collected;
            this.collectedByPlayerId = collectedByPlayerId;
        }
    }
}
