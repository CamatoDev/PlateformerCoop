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
        // Déplacement horizontal simple
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            velocity.x = Constants.PLAYER_SPEED;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            velocity.x = -Constants.PLAYER_SPEED;
        } else {
            velocity.x = 0;
        }

        // Saut aussi avec Z ou UP (pour convenances)
        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) && isGrounded) {
            velocity.y = Constants.JUMP_FORCE;
            isGrounded = false;
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
        if (position.x > Constants.VIEWPORT_WIDTH - Constants.PLAYER_WIDTH) {
            position.x = Constants.VIEWPORT_WIDTH - Constants.PLAYER_WIDTH;
        }

        // Sol (temporaire - sera remplacé par les plateformes)
        if (position.y < 0) {
            position.y = 0;
            velocity.y = 0;
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
