package com.vortexmakers.plateformer.entities;


import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Collectible implements GameEntity {
    // POSITION ET GÉOMÉTRIE
    private Vector2 position;     // (coordonnées monde)
    private Rectangle bounds;

    // ÉTATS
    private boolean collected;    // le collectible a été pris
    private boolean animating;    // joue l'animation de collecte
    private float animationTimer; // Compteur pour l'animation

    // VISUEL
    private ShapeRenderer debugRenderer;

    // CONSTANTES
    private static final float SIZE = 24f;          // Taille du collectible
    private static final float ANIMATION_DURATION = 0.3f; // Durée animation collecte

    public Collectible(float x, float y) {
        this.position = new Vector2(x, y);
        this.bounds = new Rectangle(x, y, SIZE, SIZE);
        debugRenderer = new ShapeRenderer();
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
            if (animationTimer >= ANIMATION_DURATION) {
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
        if (animating) {
            // Animation de "pop" : le collectible grossit puis disparaît
            float progress = animationTimer / ANIMATION_DURATION;
            scale = 1.0f + progress * 0.5f; // Grossit de 50%
        }
        // Calculer la taille animée
        float animatedSize = SIZE * scale;

        // Calculer le décalage pour centrer l'animation
        float offset = (animatedSize - SIZE) / 2;

        batch.end();
        debugRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Alpha réduit pendant l'animation pour l'effet de disparition
        float alpha = animating ? (1.0f - (animationTimer / ANIMATION_DURATION)) : 1.0f;
        debugRenderer.setColor(1f, 1f, 0f, alpha); // Jaune pour les collectibles
        debugRenderer.rect(position.x, position.y, animatedSize, animatedSize);
        debugRenderer.end();
        batch.begin();
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
        debugRenderer.dispose();
    }
}
