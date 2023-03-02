package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.handlers.HomeHandler;
import org.server.app.handlers.AuthHandler;
import org.server.app.utils.HandlerBuilder;

public class AuthRoute {
    private final HttpServer server;
    private final String basePath;

    public AuthRoute(HttpServer server, String basePath) {
        this.server = server;
        this.basePath = basePath;
    }

    public void registerRoutes() {
        AuthHandler authHandler = new AuthHandler();
        HttpHandler loginHandler = new HandlerBuilder(authHandler.loginHandler).build();
        HttpHandler signinHandler = new HandlerBuilder(authHandler.signInHandler).build();
        this.server.createContext(basePath + "/login", loginHandler);
        this.server.createContext(basePath + "/signin", signinHandler);
    }
}
