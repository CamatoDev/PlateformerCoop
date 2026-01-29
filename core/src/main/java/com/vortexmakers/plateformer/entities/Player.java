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
import com.vortexmakers.plateformer.utils.PlayerTextures;

public class Player implements GameEntity {
    // TYPE DE PERSONNAGE
    private String characterType;
    private PlayerTextures textures;

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
        this(startX, startY, "beige"); // Par défaut : beige
    }

    /**
     * CONSTRUCTEUR AVEC CHOIX DE PERSONNAGE
     */
    public Player(float startX, float startY, String characterType) {
        position = new Vector2(startX, startY);
        velocity = new Vector2();
        bounds = new Rectangle(position.x, position.y, Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT);
        isGrounded = false;

        // Sauvegarder le type
        this.characterType = characterType;

        // RÉCUPÉRATION DE L'ASSETMANAGER
        this.assets = AssetManager.getInstance();

        // Charger les textures du personnage choisi
        this.textures = assets.getCharacterTextures(characterType);

        // TEXTURE INITIALE
        this.currentTexture = textures.idle;
        this.currentFrame = new TextureRegion(currentTexture);

        System.out.println("Player créé avec personnage : " + characterType);
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
            isGrounded = true;
        }
    }

    public void updateBounds() {
        bounds.setPosition(position);
    }

    public void updateAnimation(float delta) {
        // Détection d'état claire
        boolean isMovingHorizontally = Math.abs(velocity.x) > 5f;

        // CORRECTION PRINCIPALE : Priorité claire des animations
        if (!isGrounded) {
            // EN L'AIR : Afficher toujours le saut
            if (currentTexture != textures.jump) {
                currentTexture = textures.jump;
            }

        } else {
            // AU SOL : Idle ou marche
            if (isMovingHorizontally) {
                // ANIMATION DE MARCHE
                walkAnimationTimer += delta;
                if (walkAnimationTimer >= Constants.WALK_ANIMATION_SPEED) {
                    walkAnimationTimer = 0f;

                    if (currentTexture == textures.walkA) {
                        currentTexture = textures.walkB;
                    } else {
                        currentTexture = textures.walkA;
                    }
                }
            } else {
                // ANIMATION IDLE
                currentTexture = textures.idle;
                walkAnimationTimer = 0f;
            }
        }

        // Gestion direction
        if (velocity.x > 1f) {
            facingRight = true;
        } else if (velocity.x < -1f) {
            facingRight = false;
        }

        // Application direction
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

    /**
     * CHANGER LE TYPE DE PERSONNAGE
     * Utile si on veut permettre de changer en jeu (futur)
     */
    public void setCharacterType(String newCharacterType) {
        if (!assets.hasCharacter(newCharacterType)) {
            System.err.println("Personnage inconnu : " + newCharacterType);
            return;
        }

        this.characterType = newCharacterType;
        this.textures = assets.getCharacterTextures(newCharacterType);
        this.currentTexture = textures.idle;
        this.currentFrame = new TextureRegion(currentTexture);

        System.out.println("Personnage changé vers : " + newCharacterType);
    }

    public String getCharacterType() {
        return characterType;
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
        // Nettoyer les références pour le GC
        currentTexture = null;
        currentFrame = null;
        assets = null;

        System.out.println("Player nettoyé (références libérées)");
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
