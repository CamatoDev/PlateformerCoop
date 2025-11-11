package com.vortexmakers.plateformer.entities;


import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public interface GameEntity {
    void update(float deltaTime);
    void render(SpriteBatch batch);
    Rectangle getBounds();
}
