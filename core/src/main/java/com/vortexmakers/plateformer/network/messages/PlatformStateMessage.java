package com.vortexmakers.plateformer.network.messages;

import java.util.ArrayList;
import java.util.List;

public class PlatformStateMessage extends NetworkMessage {
    public List<PlatformData> platforms;

    public PlatformStateMessage() {
        this.platforms = new ArrayList<>();
    }

    public static class PlatformData {
        public float x, y, width, height;
        public int type; // 0 = terrain, 1 = block (pour la texture)

        public PlatformData() {
        }

        public PlatformData(float x, float y, float width, float height, int type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
        }
    }
}
