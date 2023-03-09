package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.controllers.AuthController;
import org.server.app.controllers.ConfigController;
import org.server.app.utils.AuthorizationHandler;
import org.server.app.utils.HandlerBuilder;

public class ConfigRoute {
    private final HttpServer server;
    private final String basePath;

    public ConfigRoute(HttpServer server, String basePath) {
        this.server = server;
        this.basePath = basePath;
    }

    public void registerRoutes() {
        ConfigController configController = new ConfigController();
        HttpHandler configHandler = new HandlerBuilder(configController.configHandler)
                .setMiddlewareHandler(new AuthorizationHandler()).build();

        this.server.createContext(basePath , configHandler);
    }
}
