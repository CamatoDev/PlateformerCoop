package com.vortexmakers.plateformer.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.vortexmakers.plateformer.utils.AssetManager;

/**
 * DRAPEAU DE FIN DE NIVEAU
 * Anime entre deux textures pour créer un effet de mouvement
 */
public class FinishFlag implements GameEntity {
    private Vector2 position;
    private Rectangle bounds;
    private AssetManager assets;

    // Animation
    private Texture flagTextureA;
    private Texture flagTextureB;
    private Texture currentTexture;
    private float animationTimer = 0f;
    private static final float ANIMATION_SPEED = 0.3f; // Change toutes les 0.3s

    // Taille du drapeau (128x128 pixels art → 64x64 monde)
    private static final float FLAG_WIDTH = 64f;
    private static final float FLAG_HEIGHT = 64f;

    public FinishFlag(float x, float y) {
        this.position = new Vector2(x, y);
        this.bounds = new Rectangle(x, y, FLAG_WIDTH, FLAG_HEIGHT);

        this.assets = AssetManager.getInstance();
        this.flagTextureA = assets.getFlagGreenA();
        this.flagTextureB = assets.getFlagGreenB();
        this.currentTexture = flagTextureA; // Commence avec A
    }

    @Override
    public void update(float deltaTime) {
        // Animation : alterner entre les deux textures
        animationTimer += deltaTime;

        if (animationTimer >= ANIMATION_SPEED) {
            animationTimer = 0f;

            // Alterner la texture
            if (currentTexture == flagTextureA) {
                currentTexture = flagTextureB;
            } else {
                currentTexture = flagTextureA;
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        // Échelle : 128 pixels art → 64 pixels monde
        float scale = FLAG_WIDTH / 128f;

        batch.draw(
            currentTexture,
            position.x,
            position.y,
            0, 0,                    // Origin
            FLAG_WIDTH,
            FLAG_HEIGHT,
            scale,
            scale,
            0,                       // Rotation
            0, 0,                    // Source X, Y
            128, 128,                // Source width, height
            false, false             // Flip X, Y
        );
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    public Vector2 getPosition() {
        return position;
    }

    public void dispose() {
        // Ne pas disposer les textures, c'est AssetManager qui gère
        flagTextureA = null;
        flagTextureB = null;
        currentTexture = null;
        assets = null;
    }
}
