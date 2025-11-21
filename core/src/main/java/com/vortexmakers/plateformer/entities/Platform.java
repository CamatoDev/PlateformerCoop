package com.vortexmakers.plateformer.entities;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.vortexmakers.plateformer.utils.Constants;
import com.vortexmakers.plateformer.utils.AssetManager;

public class Platform implements GameEntity {
    private Rectangle bounds;
    private AssetManager assets;
    private Texture platformTexture;

    public Platform(float x, float y, float width) {
        bounds = new Rectangle(x, y, width, Constants.PLATFORM_HEIGHT);
        this.assets = AssetManager.getInstance();

        // CHOIX DE LA TEXTURE
        this.platformTexture = assets.getPlatformTerrain();
    }
    public Platform(float x, float y, float width, float height) {
        bounds = new Rectangle(x, y, width, height);
        this.assets = AssetManager.getInstance();

        // CHOIX DE LA TEXTURE
        this.platformTexture = assets.getPlatformBlock();
    }

    @Override
    public void update(float deltaTime) {
        // Les plateformes statiques n'ont pas besoin de update
    }

    @Override
    public void render(SpriteBatch batch) {
        // TAILLE D'UNE TUILE (64px art → 16px monde)
        float tileSize = Constants.PLATFORM_HEIGHT; // 16px monde
        float artToWorldScale = tileSize / 64f;     // 64px art → 16px monde

        // NOMBRE DE TUILES NÉCESSAIRES
        int tileCount = (int) Math.ceil(bounds.width / tileSize);

        // DESSINER CHAQUE TUILE
        for (int i = 0; i < tileCount; i++) {
            float x = bounds.x + (i * tileSize);
            float width = Math.min(tileSize, bounds.x + bounds.width - x);

            batch.draw(
                platformTexture,
                x,
                bounds.y,
                width,
                bounds.height,
                0, 0,                                  // Region X, Y
                (int)(width / artToWorldScale),        // Region width (pixels art)
                64,                                    // Region height (pixels art)
                false, false                           // Flip X, Y
            );
        }
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    public void dispose() {

    }
}
