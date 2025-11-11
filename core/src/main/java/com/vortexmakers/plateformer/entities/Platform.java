package com.vortexmakers.plateformer.entities;


import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.vortexmakers.plateformer.utils.Constants;

public class Platform implements GameEntity {
    private Rectangle bounds;
    private ShapeRenderer debugRenderer;

    public Platform(float x, float y, float width) {
        bounds = new Rectangle(x, y, width, Constants.PLATFORM_HEIGHT);
        debugRenderer = new ShapeRenderer();
    }

    @Override
    public void update(float deltaTime) {
        // Les plateformes statiques n'ont pas besoin de update
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.end(); // Temporairement

        debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
        debugRenderer.setColor(0.2f, 0.8f, 0.2f, 1); // Vert
        debugRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        debugRenderer.end();

        batch.begin();
    }

    @Override
    public Rectangle getBounds() { return bounds; }

    public void dispose() {
        debugRenderer.dispose();
    }
}
