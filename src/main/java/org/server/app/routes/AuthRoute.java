package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.controllers.AuthController;
import org.server.app.utils.HandlerBuilder;

public class AuthRoute {
    private final HttpServer server;
    private final String basePath;

    public AuthRoute(HttpServer server, String basePath) {
        this.server = server;
        this.basePath = basePath;
    }

    public void registerRoutes() {
        AuthController authController = new AuthController();
        HttpHandler loginHandler = new HandlerBuilder(authController.loginHandler).build();
        HttpHandler signinHandler = new HandlerBuilder(authController.signInHandler).build();
        this.server.createContext(basePath + "/login", loginHandler);
        this.server.createContext(basePath + "/signin", signinHandler);
    }
}
