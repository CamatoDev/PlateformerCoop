package com.vortexmakers.plateformer.utils;


public class Constants {
    // TAILLE DE LA VUE (ce que la caméra voit)
    public static final int VIEWPORT_WIDTH = 800;
    public static final int VIEWPORT_HEIGHT = 480;

    // TAILLE DU MONDE (optionnel - pour les limites)
    public static final int WORLD_WIDTH = 2500; // Monde plus large que la vue

    // Physique
    public static final float GRAVITY = -900f;
    public static final float JUMP_FORCE = 400f;
    //public static final float PLAYER_SPEED = 200f;
    public static final float PLAYER_ACCELERATION = 800f;
    public static final float PLAYER_DECELERATION = 600f;
    public static final float MAX_PLAYER_SPEED = 250f;
    public static final float AIR_CONTROL_FACTOR = 0.6f; // Contrôle réduit en l'air

    // Tailles
    public static final float PLAYER_WIDTH = 32f;
    public static final float PLAYER_HEIGHT = 32f;
    public static final float PLATFORM_HEIGHT = 32f;

    // Caméra
    public static final float CAMERA_LEAD = 200f; // Avance de la caméra
    public static final float CAMERA_SMOOTHNESS = 5f; // Lissage du mouvement
}
