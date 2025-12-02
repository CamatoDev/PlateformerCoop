package com.vortexmakers.plateformer.network;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.vortexmakers.plateformer.utils.Constants;

/**
 * JOUEUR SERVEUR - Représentation légère pour la physique serveur
 * Sans graphiques, seulement logique
 */
public class ServerPlayer {
    public float x, y;
    public float velocityX, velocityY;
    public boolean isGrounded;
    public int playerId;

    // Inputs courants
    public float currentInputX = 0;
    public boolean currentJumpPressed = false;

    // Pour collisions
    private Rectangle bounds;

    // ✅ NOUVEAU : Compteur pour garder l'input de saut
    private int jumpBufferFrames = 0;
    private static final int JUMP_BUFFER_DURATION = 3; // Garder pendant 3 frames (~50ms)

    public ServerPlayer(float startX, float startY, int playerId) {
        this.x = startX;
        this.y = startY;
        this.velocityX = 0;
        this.velocityY = 0;
        this.isGrounded = false;
        this.playerId = playerId;
        this.bounds = new Rectangle(x, y, Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT);
    }

    public void updateBounds() {
        bounds.setPosition(x, y);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Vector2 getPosition() {
        return new Vector2(x, y);
    }

    /**
     * APPLIQUER LA PHYSIQUE SERVEUR AVEC GRAVITÉ CORRECTE
     */
    public void applyServerPhysics(float delta) {
        // GRAVITÉ
        if (!isGrounded) {
            velocityY += Constants.GRAVITY * delta;
        } else {
            velocityY = 0;
        }

        // MOUVEMENT HORIZONTAL (inchangé)
        float targetVelocityX = currentInputX * Constants.MAX_PLAYER_SPEED;
        float controlFactor = isGrounded ? 1.0f : Constants.AIR_CONTROL_FACTOR;

        if (currentInputX != 0) {
            if (velocityX < targetVelocityX) {
                velocityX = Math.min(velocityX + Constants.PLAYER_ACCELERATION * delta * controlFactor, targetVelocityX);
            } else if (velocityX > targetVelocityX) {
                velocityX = Math.max(velocityX - Constants.PLAYER_ACCELERATION * delta * controlFactor, targetVelocityX);
            }
        } else {
            if (velocityX > 0) {
                velocityX = Math.max(velocityX - Constants.PLAYER_DECELERATION * delta, 0);
            } else if (velocityX < 0) {
                velocityX = Math.min(velocityX + Constants.PLAYER_DECELERATION * delta, 0);
            }

            if (Math.abs(velocityX) < 10f) {
                velocityX = 0;
            }
        }

        // ✅ CORRECTION : Gérer le buffer de saut
        if (currentJumpPressed) {
            jumpBufferFrames = JUMP_BUFFER_DURATION; // Activer le buffer
            currentJumpPressed = false; // Consommer l'input immédiatement
        }

        // ✅ NOUVEAU : Décrémenter le buffer
        if (jumpBufferFrames > 0) {
            jumpBufferFrames--;
        }

        // ✅ CORRECTION : Saut avec buffer
        if (jumpBufferFrames > 0 && isGrounded) {
            velocityY = Constants.JUMP_FORCE;
            isGrounded = false;
            jumpBufferFrames = 0; // Consommer le buffer
            System.out.println("🚀 Saut exécuté pour joueur " + playerId);
        }

        // APPLICATION DU MOUVEMENT
        x += velocityX * delta;
        y += velocityY * delta;

        updateBounds();

        // LIMITES DU MONDE
        if (x < 0) {
            x = 0;
            velocityX = 0;
        }
        if (x > Constants.WORLD_WIDTH - Constants.PLAYER_WIDTH) {
            x = Constants.WORLD_WIDTH - Constants.PLAYER_WIDTH;
            velocityX = 0;
        }

        // SOL DE SECOURS
        if (y < 0) {
            y = 0;
            velocityY = 0;
            isGrounded = true;
        }
    }
}
