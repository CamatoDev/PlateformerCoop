package com.vortexmakers.plateformer.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.vortexmakers.plateformer.utils.AssetManager;

/**
 * SPIKE - Piège mortel qui déclenche le respawn du joueur
 *
 * Caractéristiques :
 * - Statique (ne bouge pas)
 * - Collision = mort instantanée
 * - Texture 64x64 pixels art
 */
public class Spike implements GameEntity {
    private Vector2 position;
    private Rectangle bounds;
    private AssetManager assets;
    private Texture spikeTexture;

    // Taille d'un spike (64 pixels art → 32 pixels monde)
    public static final float SPIKE_SIZE = 32f;

    /**
     * CONSTRUCTEUR
     * @param x Position X dans le monde
     * @param y Position Y dans le monde
     */
    public Spike(float x, float y) {
        this.position = new Vector2(x, y);
        this.bounds = new Rectangle(x, y, SPIKE_SIZE, SPIKE_SIZE);

        // Récupération de la texture
        this.assets = AssetManager.getInstance();
        this.spikeTexture = assets.getSpikeTexture();

        System.out.println("Spike créé à X: " + x + ", Y: " + y);
    }

    @Override
    public void update(float deltaTime) {
        // Les spikes sont statiques, pas de mise à jour nécessaire
    }

    @Override
    public void render(SpriteBatch batch) {
        // Échelle : 64 pixels art → 32 pixels monde
        float scale = SPIKE_SIZE / 64f;

        batch.draw(
            spikeTexture,
            position.x,
            position.y,
            0, 0,                    // Origin
            SPIKE_SIZE,              // Largeur destination
            SPIKE_SIZE,              // Hauteur destination
            scale,                   // Échelle X
            scale,                   // Échelle Y
            0,                       // Rotation
            0, 0,                    // Source X, Y
            64, 64,                  // Source width, height (64x64 art)
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

    /**
     * VÉRIFIER SI UN JOUEUR TOUCHE CE SPIKE
     */
    public boolean isCollidingWith(Rectangle playerBounds) {
        return bounds.overlaps(playerBounds);
    }

    public void dispose() {
        // AssetManager gère les textures, juste nettoyer les références
        spikeTexture = null;
        assets = null;
    }
}
