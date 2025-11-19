package com.vortexmakers.plateformer.utils;


public class Constants {
    // ÉCHELLE DU MONDE vs ÉCHELLE AFFICHAGE
    public static final float WORLD_TO_SCREEN = 1.0f; // 1 unité monde = 1 pixel écran

    // RÉSOLUTION DE JEU FIXE (logique interne)
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 480;

    // TAILLE RÉELLE DE LA FENÊTRE
    public static int SCREEN_WIDTH = 1080;
    public static int SCREEN_HEIGHT = 720;

    // TAILLE DU MONDE (pour les limites)
    public static final int WORLD_WIDTH = 3000; // Monde plus large que la vue

    // CONSTANTES DE BACKGROUND
    public static final float CLOUD_SCROLL_SPEED = 15f;
    public static final float CLOUD_SCALE = 0.4f;    // Nuages plus petits
    public static final float CLOUD_Y_POSITION = 0.55f; // 55% du haut de l'écran
    public static final float TREES_Y_POSITION = 0.05f; // 5% du bas de l'écran

    // Physique
    public static final float GRAVITY = -900f;
    public static final float JUMP_FORCE = 400f;
    public static final float PLAYER_ACCELERATION = 800f;
    public static final float PLAYER_DECELERATION = 600f;
    public static final float MAX_PLAYER_SPEED = 250f;
    public static final float WALK_ANIMATION_SPEED = 0.15f; // Secondes entre chaque frame
    public static final float AIR_CONTROL_FACTOR = 0.6f; // Contrôle réduit en l'air

    // Tailles
    public static final float PLAYER_WIDTH = 32f;
    public static final float PLAYER_HEIGHT = 32f;
    public static final float PLATFORM_HEIGHT = 32f;

    // Collectibles
    public static final float COLLECTIBLE_SIZE = 24f;          // Taille du collectible
    public static final float ANIMATION_DURATION = 0.3f; // Durée animation collecte

    // Caméra
    public static final float CAMERA_LEAD = 200f; // Avance de la caméra
    public static final float CAMERA_SMOOTHNESS = 5f; // Lissage du mouvement
}
