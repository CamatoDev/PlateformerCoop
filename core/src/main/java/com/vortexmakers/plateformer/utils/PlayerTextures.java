package com.vortexmakers.plateformer.utils;

import com.badlogic.gdx.graphics.Texture;

/**
 * CONTENEUR POUR LES TEXTURES D'UN PERSONNAGE
 * Regroupe toutes les animations d'un type de personnage
 */
public class PlayerTextures {
    public Texture idle;
    public Texture jump;
    public Texture walkA;
    public Texture walkB;
    public Texture hit;

    public PlayerTextures(Texture idle, Texture jump, Texture walkA, Texture walkB, Texture hit) {
        this.idle = idle;
        this.jump = jump;
        this.walkA = walkA;
        this.walkB = walkB;
        this.hit = hit;
    }

    /**
     * Libérer toutes les textures
     */
    public void dispose() {
        if (idle != null) idle.dispose();
        if (jump != null) jump.dispose();
        if (walkA != null) walkA.dispose();
        if (walkB != null) walkB.dispose();
        if (hit != null) hit.dispose();
    }
}
