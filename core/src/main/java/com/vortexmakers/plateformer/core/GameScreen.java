package com.vortexmakers.plateformer.core;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    public GameScreen(PlateformerGame game) {
        this.game = game;

        // Configuration caméra
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);

        batch = new SpriteBatch();

        // Création des entités
        player = new Player(100, 300);
        platforms = new Array<>();
        createTestLevel();

        physicsSystem = new PhysicsSystem();
    }

    private void createTestLevel() {
        // Plateforme de base (sol)
        platforms.add(new Platform(0, 0, Constants.VIEWPORT_WIDTH));

        // Quelques plateformes de test
        platforms.add(new Platform(200, 90, 100));
        platforms.add(new Platform(400, 250, 100));
        platforms.add(new Platform(100, 350, 80));
        platforms.add(new Platform(600, 200, 120));
        platforms.add(new Platform(500, Constants.PLATFORM_HEIGHT, 50, 80));
    }

    @Override
    public void render(float delta) {
        // Mise à jour
        update(delta);

        // Rendu
        batch.setProjectionMatrix(camera.combined);
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
        // Caméra suit le joueur
        camera.position.x = player.getPosition().x + 150; // Légère avance
        camera.position.y = Constants.VIEWPORT_HEIGHT / 2;
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = Constants.VIEWPORT_WIDTH;
        camera.viewportHeight = Constants.VIEWPORT_HEIGHT * ((float)height / width);
        camera.update();
    }

    @Override
    public void show() {

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
    }
}
