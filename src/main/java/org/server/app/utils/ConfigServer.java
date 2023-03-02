package org.server.app.utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Paths;

public class ConfigServer {
    static ConfigServer instance;
    public Dotenv dotenv;

    private ConfigServer() {
        this.dotenv = Dotenv.configure().directory(Paths.get("resources").toAbsolutePath().toString()).load();
    }

    public static ConfigServer getInstance() {
        if (instance != null) return instance;
        instance = new ConfigServer();
        return instance;
    }
}
