package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.vortexmakers.plateformer.entities.Player;
import com.vortexmakers.plateformer.entities.Platform;
import com.vortexmakers.plateformer.systems.PhysicsSystem;
import com.vortexmakers.plateformer.utils.Constants;

public class GameScreen implements Screen {
    private final PlateformerGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;

    private Player player;
    private Array<Platform> platforms;
    private PhysicsSystem physicsSystem;
    private ShapeRenderer debugRenderer;

    public GameScreen(PlateformerGame game) {
        this.game = game;

        // Configuration caméra
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
        // Position initiale de la caméra
        camera.position.set(Constants.VIEWPORT_WIDTH / 2, Constants.VIEWPORT_HEIGHT / 2, 0);
        camera.update();

        batch = new SpriteBatch();

        debugRenderer = new ShapeRenderer();

        // Création des entités
        player = new Player(100, 300);
        platforms = new Array<>();
        createTestLevel();

        physicsSystem = new PhysicsSystem();

        System.out.println("=== INITIALISATION CAMÉRA ===");
        System.out.println("Viewport: " + Constants.VIEWPORT_WIDTH + "x" + Constants.VIEWPORT_HEIGHT);
        System.out.println("Position caméra: " + camera.position.x + ", " + camera.position.y);
    }

    private void createTestLevel() {
        // Plateforme de base (sol)
        //platforms.add(new Platform(0, 0, Constants.VIEWPORT_WIDTH));
        // Plateforme de base (sol)
        platforms.add(new Platform(0, 0, 150));
        platforms.add(new Platform(216, 0, 150));
        platforms.add(new Platform(216 * 2, 0, 150));
        platforms.add(new Platform(216 * 3, 0, 150));
        platforms.add(new Platform(216 * 4, 0, 400));

        // Quelques plateformes de test
        platforms.add(new Platform(200, 80, 100));
        platforms.add(new Platform(550, 140, 100));
        platforms.add(new Platform(100, 160, 85));
        platforms.add(new Platform(700, 80, 120));
        platforms.add(new Platform(500, Constants.PLATFORM_HEIGHT, 50, 80));

        // Nouvelle plateforme loin à droite
        platforms.add(new Platform(1150, 80, 400));
        platforms.add(new Platform(1400, 140, 100));
        platforms.add(new Platform(1700, 160, 150));

        // Ajoutons encore plus de plateformes pour vraiment voir le défilement
        platforms.add(new Platform(2000, 200, 200));
        platforms.add(new Platform(2300, 100, 150));
    }

    @Override
    public void render(float delta) {
        // Mise à jour
        update(delta);

        // Mise à jour de la caméra
        updateCamera();

        // Rendu
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // DEBUG : Fond coloré pour voir les limites
        // Cette partie est temporaire pour le debug
        batch.end();
        debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
        debugRenderer.setColor(0.3f, 0.5f, 0.8f, 1); // Bleu ciel
        debugRenderer.rect(camera.position.x - Constants.VIEWPORT_WIDTH/2,
            camera.position.y - Constants.VIEWPORT_HEIGHT/2,
            Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
        debugRenderer.end();
        batch.begin();

        // Dessiner les plateformes
        for (Platform platform : platforms) {
            platform.render(batch);
        }

        // Dessiner le joueur
        player.render(batch);

        batch.end();

        // Mettre à jour la caméra
        updateCamera();
    }

    private void update(float delta) {
        player.update(delta);
        physicsSystem.checkCollisions(player, platforms);
    }

    private void updateCamera() {
        // Position cible de la caméra
        float targetX = player.getPosition().x + Constants.CAMERA_LEAD;

        // Limites de la caméra
        float minCameraX = Constants.VIEWPORT_WIDTH / 2;
        float maxCameraX = Constants.WORLD_WIDTH - Constants.VIEWPORT_WIDTH / 2;

        targetX = Math.max(minCameraX, Math.min(targetX, maxCameraX));

        // Application directe (sans lissage pour debug)
        camera.position.x = targetX;
        camera.position.y = Constants.VIEWPORT_HEIGHT / 2;
        camera.update();

        // DEBUG DÉTAILLÉ
        System.out.println("=== CAMÉRA DEBUG ===");
        System.out.println("Player: " + player.getPosition().x + ", " + player.getPosition().y);
        System.out.println("Camera: " + camera.position.x + ", " + camera.position.y);
        System.out.println("Viewport: " + camera.viewportWidth + "x" + camera.viewportHeight);
        System.out.println("Zone visible: " +
            (camera.position.x - Constants.VIEWPORT_WIDTH/2) + " à " +
            (camera.position.x + Constants.VIEWPORT_WIDTH/2));
    }

//    private void updateCamera() {
//        // Position cible de la caméra (avec avance devant le joueur)
//        float targetX = player.getPosition().x + Constants.CAMERA_LEAD;
//
//        // Limiter la caméra pour ne pas montrer du vide à gauche
//        float minCameraX = Constants.VIEWPORT_WIDTH / 2;
//        targetX = Math.max(targetX, minCameraX);
//
//        // CORRECTION : Ajouter une limite maximale si tu veux
//        // float maxCameraX = Constants.WORLD_WIDTH - Constants.VIEWPORT_WIDTH / 2;
//        // targetX = Math.min(targetX, maxCameraX);
//
//        // Lissage du mouvement de caméra (évite les saccades)
//        float currentX = camera.position.x;
//        float newX = currentX + (targetX - currentX) * Constants.CAMERA_SMOOTHNESS * Gdx.graphics.getDeltaTime();
//
//        // DEBUG TEMPORAIRE
//        System.out.println("Player X: " + player.getPosition().x +
//            " | Target X: " + targetX +
//            " | Camera X: " + newX +
//            " | Viewport: " + Constants.VIEWPORT_WIDTH + "x" + Constants.VIEWPORT_HEIGHT);
//
//        // Appliquer la nouvelle position
//        camera.position.x = newX;
//        camera.position.y = Constants.VIEWPORT_HEIGHT / 2; // CORRECTION : pas de cast float
//        camera.update();
//    }


    @Override
    public void resize(int width, int height) {
        // APPROCHE SIMPLIFIÉE : Garder le viewport fixe
        System.out.println("Resize: " + width + "x" + height);

        // Calculer le ratio d'aspect
        float aspectRatio = (float)width / height;
        float desiredAspect = (float)Constants.VIEWPORT_WIDTH / Constants.VIEWPORT_HEIGHT;

        if (aspectRatio > desiredAspect) {
            // Fenêtre plus large que le viewport
            float newWidth = Constants.VIEWPORT_HEIGHT * aspectRatio;
            camera.viewportWidth = newWidth;
            camera.viewportHeight = Constants.VIEWPORT_HEIGHT;
        } else {
            // Fenêtre plus haute que le viewport
            float newHeight = Constants.VIEWPORT_WIDTH / aspectRatio;
            camera.viewportWidth = Constants.VIEWPORT_WIDTH;
            camera.viewportHeight = newHeight;
        }

        camera.update();
        System.out.println("Nouveau viewport: " + camera.viewportWidth + "x" + camera.viewportHeight);

//        float aspectRatio = (float) height / width;
//        camera.viewportWidth = Constants.VIEWPORT_WIDTH * aspectRatio;
//        camera.viewportHeight = Constants.VIEWPORT_HEIGHT * aspectRatio;
//        camera.update();
//
//        System.out.println("Fenêtre redimensionnée: " + width + "x" + height);
//        System.out.println("Viewport caméra: " + camera.viewportWidth + "x" + camera.viewportHeight);
    }

    @Override
    public void show() {
        System.out.println("GameScreen show()");
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        for (Platform platform : platforms) {
            platform.dispose();
        }
        debugRenderer.dispose();
    }
}
