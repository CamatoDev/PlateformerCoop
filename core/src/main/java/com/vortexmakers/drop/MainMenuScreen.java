package com.vortexmakers.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;

public class MainMenuScreen implements Screen {

    final Drop game;

    Texture backgroundTexture;
    Texture bucketTexture;


    public MainMenuScreen(final Drop game) {
        this.game = game;

        backgroundTexture = new Texture("background.png");
        bucketTexture = new Texture("bucket.png");
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();

        game.batch.begin();

        game.batch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        game.batch.draw(bucketTexture, worldWidth/2 - 1, worldHeight/2 - 1, 1, 1);

        //draw text. Remember that x and y are in meters
        game.font.draw(game.batch, "Welcome to Drop!!! ", worldWidth/2, worldHeight/2);
        game.font.draw(game.batch, "Tap anywhere to begin!", worldWidth/2, worldHeight/2 - .5f);
        game.batch.end();

        if (Gdx.input.isTouched()) {
            game.setScreen(new GameScreen(game));
            dispose();
        }

        if(Gdx.input.isKeyPressed(Input.Keys.ENTER)){
            game.setScreen(new GameScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
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

    }


    //...Rest of class omitted for succinctness.

}
