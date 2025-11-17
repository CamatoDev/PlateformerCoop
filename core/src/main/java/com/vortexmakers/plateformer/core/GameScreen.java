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
    private OrthographicCamera gameCamera;  // Caméra pour la logique de jeu
    private OrthographicCamera uiCamera;    // Caméra pour l'interface
    private SpriteBatch batch;

    private Player player;
    private Array<Platform> platforms;
    private PhysicsSystem physicsSystem;
    private ShapeRenderer debugRenderer;

    // Échelle d'affichage
    private float scaleX, scaleY;

    public GameScreen(PlateformerGame game) {
        this.game = game;

        // Initialiser les caméras
        gameCamera = new OrthographicCamera();
        gameCamera.setToOrtho(false, Constants.GAME_WIDTH, Constants.GAME_HEIGHT);
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);

        batch = new SpriteBatch();

        debugRenderer = new ShapeRenderer();

        // Création des entités
        player = new Player(50, 300);
        platforms = new Array<>();
        createTestLevel();

        physicsSystem = new PhysicsSystem();
    }

    private void createTestLevel() {
        // Plateforme de base (sol)
        //platforms.add(new Platform(0, 0, Constants.VIEWPORT_WIDTH));
        // Plateforme de base (sol)
//        platforms.add(new Platform(0, 0, 150));
//        platforms.add(new Platform(216, 0, 150));
//        platforms.add(new Platform(216 * 2, 0, 150));
//        platforms.add(new Platform(216 * 3, 0, 150));
//        platforms.add(new Platform(216 * 4, 0, 400));

        // SOL CONTINU
        for (int i = 0; i < 15; i++) {
            platforms.add(new Platform(i * 200, 0, 200)); // ou width: 150
        }

        // Quelques plateformes de test
        platforms.add(new Platform(200, 80, 100));
        platforms.add(new Platform(580, 150, 100));
        platforms.add(new Platform(100, 160, 85));
        platforms.add(new Platform(720, 80, 120));
        platforms.add(new Platform(500, Constants.PLATFORM_HEIGHT, 50, 80));

        // Nouvelle plateforme loin à droite
        platforms.add(new Platform(1150, 80, 400));
        platforms.add(new Platform(1400, 160, 100));
        platforms.add(new Platform(1700, 160, 150));

        // Ajoutons encore plus de plateformes pour vraiment voir le défilement
        platforms.add(new Platform(2000, 200, 200));
        platforms.add(new Platform(2300, 100, 150));
        platforms.add(new Platform(2600, 150, 120));
        platforms.add(new Platform(2900, 80, 200));
    }

    @Override
    public void render(float delta) {
        // Mise à jour
        update(delta);

        // Mise à jour de la caméra
        updateCamera();

        // Rendu
        batch.setProjectionMatrix(gameCamera.combined);
        batch.begin();

        batch.end();



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
        // SUIVI SIMPLE DU JOUEUR
        float targetX = player.getPosition().x + Constants.CAMERA_LEAD;

        // Limites de la caméra
        float minCameraX = Constants.GAME_WIDTH / 2;
        float maxCameraX = Constants.WORLD_WIDTH - Constants.GAME_WIDTH / 2;

        targetX = Math.max(minCameraX, Math.min(targetX, maxCameraX));

        // Application directe
        gameCamera.position.x = targetX;
        gameCamera.position.y = Constants.GAME_HEIGHT / 2;
        gameCamera.update();
    }

    @Override
    public void resize(int width, int height) {
        // METTRE À JOUR LES CONSTANTES D'ÉCRAN
        Constants.SCREEN_WIDTH = width;
        Constants.SCREEN_HEIGHT = height;

        // Recalculer l'échelle
        scaleX = (float) width / Constants.GAME_WIDTH;
        scaleY = (float) height / Constants.GAME_HEIGHT;

        // Mettre à jour la caméra UI
        uiCamera.setToOrtho(false, width, height);
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
