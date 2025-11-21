package com.vortexmakers.plateformer.network.messages;


public class PlayerInputMessage extends NetworkMessage {
    // Inputs du joueur
    public float velocityX;
    public boolean jumpPressed;
    public boolean movingLeft;
    public boolean movingRight;

    // Position actuelle (pour vérification)
    public float currentX, currentY;

    public PlayerInputMessage() {
        // Constructeur vide requis par Kryo
    }

    public PlayerInputMessage(int playerId, float velocityX, boolean jumpPressed,
                              boolean movingLeft, boolean movingRight,
                              float currentX, float currentY) {
        super(playerId);
        this.velocityX = velocityX;
        this.jumpPressed = jumpPressed;
        this.movingLeft = movingLeft;
        this.movingRight = movingRight;
        this.currentX = currentX;
        this.currentY = currentY;
    }
}
