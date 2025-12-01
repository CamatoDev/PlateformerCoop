package com.vortexmakers.plateformer.core;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */


import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.vortexmakers.plateformer.network.NetworkManager;
import com.vortexmakers.plateformer.utils.AssetManager;

public class PlateformerGame extends Game {
    // ✅ RÉFÉRENCE AU SINGLETON
    private NetworkManager networkManager;

    @Override
    public void create() {
        // ✅ INITIALISATION DU SINGLETON
        networkManager = NetworkManager.getInstance();
        setScreen(new LobbyScreen(this));
    }

    @Override
    public void render() {
        super.render();

        // ✅ TOUJOURS DISPONIBLE
        if (networkManager != null) {
            networkManager.updateServer(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void dispose() {
        System.out.println("🧹 Nettoyage global du jeu...");

        // Disposer l'écran actuel
        if (screen != null) {
            screen.dispose();
        }

        // ✅ NOUVEAU : Disposer l'AssetManager globalement
        AssetManager assetManager = AssetManager.getInstance();
        if (assetManager != null && assetManager.areAssetsLoaded()) {
            assetManager.dispose();
            System.out.println("✅ AssetManager disposé");
        }

        // Nettoyage réseau
        if (networkManager != null) {
            networkManager.disconnect();
            NetworkManager.resetInstance();
        }

        System.out.println("✅ Jeu complètement nettoyé");
    }
}
