package com.vortexmakers.plateformer.core;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */


import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class PlateformerGame extends Game {

    @Override
    public void create() {
        // Pas besoin d'assets pour l'instant, on utilise des formes simples
        setScreen(new GameScreen(this));
    }

    @Override
    public void render() {
        // Fond d'écran bleu ciel
        Gdx.gl.glClearColor(0.5f, 0.7f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render();
    }
}
