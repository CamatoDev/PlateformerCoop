package com.vortexmakers.plateformer.core;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */


import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.vortexmakers.plateformer.network.NetworkManager;

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
        // ✅ NETTOYAGE GLOBAL
        if (networkManager != null) {
            NetworkManager.resetInstance(); // Ou networkManager.disconnect()
        }
    }
}
