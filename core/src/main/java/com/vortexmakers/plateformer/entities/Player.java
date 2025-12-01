package com.vortexmakers.plateformer.entities;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.vortexmakers.plateformer.utils.AssetManager;
import com.vortexmakers.plateformer.utils.Constants;

public class Player implements GameEntity {
    private Vector2 position;
    private Vector2 velocity;
    private Rectangle bounds;
    private boolean isGrounded;

    // TEXTURES ET ANIMATIONS
    private Texture currentTexture;
    private TextureRegion currentFrame;
    private boolean facingRight = true; // Direction du personnage

    // ANIMATION DE MARCHE
    private float walkAnimationTimer = 0f;
    private boolean isWalking = false;

    // RÉFÉRENCE À L'ASSETMANAGER
    private AssetManager assets;

    public Player(float startX, float startY) {
        position = new Vector2(startX, startY);
        velocity = new Vector2();
        bounds = new Rectangle(position.x, position.y, Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT);
        isGrounded = false;

        // RÉCUPÉRATION DE L'ASSETMANAGER
        this.assets = AssetManager.getInstance();

        // TEXTURE INITIALE
        this.currentTexture = assets.getPlayerIdle();
        this.currentFrame = new TextureRegion(currentTexture);

        System.out.println("Player créé avec textures");
    }

    @Override
    public void update(float deltaTime) {
        handleInput();
        applyPhysics(deltaTime);
        updateBounds();
        updateAnimation(deltaTime);
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
        if (position.x > Constants.WORLD_WIDTH - Constants.PLAYER_WIDTH) {
            position.x = Constants.WORLD_WIDTH - Constants.PLAYER_WIDTH;
        }

        // Sol (temporaire - sera remplacé par les plateformes)
        if (position.y < 0) {
            //position.y = 0;
            //velocity.y = 0;
            isGrounded = true;
        }
    }

    public void updateBounds() {
        bounds.setPosition(position);
    }

    public void updateAnimation(float delta) {
        // ✅ ÉTATS CLAIRS ET DISTINCTS
        boolean isMoving = Math.abs(velocity.x) > 5f; // Seuil plus élevé
        boolean isInAir = !isGrounded;
        boolean isActuallyJumping = isInAir && velocity.y > 0; // Monter = saut

        // ✅ LOGIQUE D'ANIMATION SIMPLIFIÉE
        if (isInAir) {
            if (isActuallyJumping) {
                currentTexture = assets.getPlayerJump();
            } else {
                currentTexture = assets.getPlayerJump(); // Ou une texture de chute si disponible
            }
            walkAnimationTimer = 0f; // Reset timer en l'air
        } else if (isMoving) {
            // ANIMATION DE MARCHE
            walkAnimationTimer += delta;
            if (walkAnimationTimer >= Constants.WALK_ANIMATION_SPEED) {
                walkAnimationTimer = 0f;
                // ALTERNANCE DES TEXTURES DE MARCHE
                if (currentTexture == assets.getPlayerWalkA()) {
                    currentTexture = assets.getPlayerWalkB();
                } else {
                    currentTexture = assets.getPlayerWalkA();
                }
            }
        } else {
            // IDLE
            currentTexture = assets.getPlayerIdle();
            walkAnimationTimer = 0f;
        }

        // ✅ GESTION DE LA DIRECTION
        if (velocity.x > 1f) {
            facingRight = true;
        } else if (velocity.x < -1f) {
            facingRight = false;
        }

        // ✅ APPLICATION DE LA DIRECTION
        currentFrame = new TextureRegion(currentTexture);
        if ((facingRight && currentFrame.isFlipX()) || (!facingRight && !currentFrame.isFlipX())) {
            currentFrame.flip(true, false);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        // Calcule de l'échelle : 128 pixels art → 32 pixels monde
        float scale = Constants.PLAYER_WIDTH / 128f;

        // Dessiner la texture avec la bonne échelle et direction
        batch.draw(
            currentFrame,                           // Texture à dessiner
            position.x,                             // Position X monde
            position.y,                             // Position Y monde
            0,                                      // Origin X (0 = coin bas-gauche)
            0,                                      // Origin Y
            Constants.PLAYER_WIDTH + 128,                 // Largeur destination
            Constants.PLAYER_HEIGHT + 128,                // Hauteur destination
            scale,                                  // Échelle X
            scale,                                  // Échelle Y
            0                                       // Rotation
        );
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

    }

    // GETTERS ET SETTERS POUR LES JOUEURS DISTANTS

    public void setPosition(float x, float y) {
        position.set(x, y);
    }

    public void setVelocity(float velX, float velY) {
        velocity.set(velX, velY);
    }

    public boolean isJumping() {
        // Adapte selon ta logique de saut
        return velocity.y > 0 || !isGrounded;
    }

    public boolean isMovingLeft() {
        return velocity.x < 0;
    }

    public boolean isMovingRight() {
        return velocity.x > 0;
    }
}
