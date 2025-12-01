package com.vortexmakers.plateformer.network.messages;

public class PlayerInputMessage extends NetworkMessage {
    // ✅ CORRECTION: Inputs bruts seulement
    public boolean leftPressed;
    public boolean rightPressed;
    public boolean jumpPressed;
    public int inputSequence;

    public PlayerInputMessage() {
        // Constructeur vide requis par Kryo
    }

    public PlayerInputMessage(int playerId, boolean leftPressed, boolean rightPressed,
                              boolean jumpPressed, int inputSequence) {
        super(playerId);
        this.leftPressed = leftPressed;
        this.rightPressed = rightPressed;
        this.jumpPressed = jumpPressed;
        this.inputSequence = inputSequence;
    }
}
