package com.vortexmakers.plateformer.entities;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.vortexmakers.plateformer.utils.AssetManager;
import com.vortexmakers.plateformer.utils.Constants;

public class Collectible implements GameEntity {
    // POSITION ET GÉOMÉTRIE
    private Vector2 position;     // (coordonnées monde)
    private Rectangle bounds;
    private AssetManager assets;
    private Texture coinTexture;

    // ÉTATS
    private boolean collected;    // le collectible a été pris
    private boolean animating;    // joue l'animation de collecte
    private float animationTimer; // Compteur pour l'animation

    // VISUEL
    //private ShapeRenderer debugRenderer;


    public Collectible(float x, float y) {
        this.position = new Vector2(x, y);
        this.bounds = new Rectangle(x, y, Constants.COLLECTIBLE_SIZE , Constants.COLLECTIBLE_SIZE );

        this.assets = AssetManager.getInstance();
        this.coinTexture = assets.getCoinTexture();

        this.collected = false;   // Pas encore collecté
        this.animating = false;   // Pas en animation
        this.animationTimer = 0f; // Timer à zéro
    }

    @Override
    public void update(float deltaTime) {
        // Si le collectible est en train d'être animé (après collecte)
        if (animating) {
            // Incrémenter le timer d'animation
            animationTimer += deltaTime;

            // Vérifier si l'animation est terminée
            if (animationTimer >= Constants.ANIMATION_DURATION) {
                // Marquer comme complètement collecté
                collected = true;
                animating = false;

                System.out.println("Animation collectible terminée");
            }
        }
        // Sinon les collectibles sont statiques, rien à mettre à jour
    }

    @Override
    public void render(SpriteBatch batch) {
        if (collected) return; // Ne pas afficher si déjà collecté

        // Calculer l'échelle pour l'animation (si en cours d'animation)
        float scale = 1.0f;
        float alpha = 1.0f;
        if (animating) {
            // Animation de "pop" : le collectible grossit puis disparaît
            float progress = animationTimer / Constants.ANIMATION_DURATION;
            scale = 1.0f + progress * 0.5f; // Grossit de 50%
            alpha = 1.0f - progress;         // Disparition
        }

        // DESSIN AVEC ANIMATION
        batch.setColor(1, 1, 1, alpha); // Appliquer alpha

        batch.draw(
            coinTexture,
            position.x - (Constants.COLLECTIBLE_SIZE * (scale - 1)) / 2, // Centrer l'animation
            position.y - (Constants.COLLECTIBLE_SIZE * (scale - 1)) / 2,
            Constants.COLLECTIBLE_SIZE * scale + 24,
            Constants.COLLECTIBLE_SIZE * scale + 24
        );

        batch.setColor(1, 1, 1, 1); // Réinitialiser alpha
    }


    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isFullyCollected() {
        return collected;
    }

    public boolean isCollecting() {
        return animating;
    }

    public boolean isAvailable() {
        return !collected && !animating;
    }

    public void collect() {
        // Vérifier qu'on ne collecte pas deux fois le même collectible
        if (!collected && !animating) {
            // Démarrer l'animation de collecte
            animating = true;
            animationTimer = 0f;

            System.out.println("Collectible collecté ! Position: " + position);

            // Ici, plus tard on pourra :
            // - Jouer un son
            // - Ajouter au score
            // - Déclencher des effets particuliers
        }
    }

    public void dispose() {
        // Nettoyer les références
        coinTexture = null;
        assets = null;
    }
}
