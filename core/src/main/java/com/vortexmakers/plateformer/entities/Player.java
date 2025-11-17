package com.vortexmakers.plateformer.entities;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.vortexmakers.plateformer.utils.Constants;

public class Player implements GameEntity {
    private Vector2 position;
    private Vector2 velocity;
    private Rectangle bounds;
    private boolean isGrounded;

    // Pour le debug visuel (carré coloré)
    private ShapeRenderer debugRenderer;

    public Player(float startX, float startY) {
        position = new Vector2(startX, startY);
        velocity = new Vector2();
        bounds = new Rectangle(position.x, position.y,
            Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT);
        isGrounded = false;

        debugRenderer = new ShapeRenderer();
    }

    @Override
    public void update(float deltaTime) {
        handleInput();
        applyPhysics(deltaTime);
        updateBounds();
    }

    private void handleInput() {
        // Récupérer l'input horizontal
        int horizontalInput = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            horizontalInput += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            horizontalInput -= 1;
        }

        // Appliquer l'accélération/décélération
        applyHorizontalMovement(horizontalInput, Gdx.graphics.getDeltaTime());

        // Saut (inchangé)
        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.W) ||
            Gdx.input.isKeyJustPressed(Input.Keys.UP)) &&
            isGrounded) {
            velocity.y = Constants.JUMP_FORCE;
            isGrounded = false;
        }
    }

    private void applyHorizontalMovement(int inputDirection, float deltaTime) {
        float targetVelocity = inputDirection * Constants.MAX_PLAYER_SPEED;

        // Facteur de contrôle (réduit en l'air)
        float controlFactor = isGrounded ? 1.0f : Constants.AIR_CONTROL_FACTOR;

        if (inputDirection != 0) {
            // ACCÉLÉRATION
            // On se rapproche de la vitesse cible
            if (velocity.x < targetVelocity) {
                velocity.x = Math.min(velocity.x + Constants.PLAYER_ACCELERATION * deltaTime * controlFactor, targetVelocity);
            } else if (velocity.x > targetVelocity) {
                velocity.x = Math.max(velocity.x - Constants.PLAYER_ACCELERATION * deltaTime * controlFactor, targetVelocity);
            }
        } else {
            // DÉCÉLÉRATION (quand aucune touche n'est pressée)
            if (velocity.x > 0) {
                velocity.x = Math.max(velocity.x - Constants.PLAYER_DECELERATION * deltaTime, 0);
            } else if (velocity.x < 0) {
                velocity.x = Math.min(velocity.x + Constants.PLAYER_DECELERATION * deltaTime, 0);
            }

            // Arrêt complet si très proche de zéro
            if (Math.abs(velocity.x) < 10f) {
                velocity.x = 0;
            }
        }
    }

    private void applyPhysics(float deltaTime) {
        // Gravité
        velocity.y += Constants.GRAVITY * deltaTime;

        // Mise à jour position
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;

        // Limites de l'écran (temporaire)
        if (position.x < 0) position.x = 0;
//        if (position.x > Constants.WORLD_WIDTH - Constants.PLAYER_WIDTH) {
//            position.x = Constants.WORLD_WIDTH - Constants.PLAYER_WIDTH;
//        }

        // Sol (temporaire - sera remplacé par les plateformes)
        if (position.y < 0) {
            //position.y = 0;
            //velocity.y = 0;
            isGrounded = true;
        }
    }

    private void updateBounds() {
        bounds.setPosition(position);
    }

    @Override
    public void render(SpriteBatch batch) {
        // Pour l'instant, on dessine un carré simple
        // On utilise ShapeRenderer car c'est plus simple que les textures pour commencer

        batch.end(); // Temporairement on stop le batch pour utiliser ShapeRenderer

        debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
        debugRenderer.setColor(0.8f, 0.2f, 0.2f, 1); // Rouge
        debugRenderer.rect(position.x, position.y, Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT);
        debugRenderer.end();

        batch.begin(); // On reprend le batch
    }

    // Getters pour les collisions
    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public void setGrounded(boolean grounded) {
        isGrounded = grounded;
    }

    public void setVelocityY(float y) {
        velocity.y = y;
    }

    public void dispose() {
        debugRenderer.dispose();
    }
}
