package com.vortexmakers.plateformer.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.vortexmakers.drop.Drop;
import com.vortexmakers.plateformer.core.PlateformerGame;


public class Lwjgl3Launcher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("Plateformer Coop");
        config.useVsync(true);
        config.setWindowedMode(1080, 720);
        config.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        config.setResizable(true);
        new Lwjgl3Application(new PlateformerGame(), config);
    }
}
