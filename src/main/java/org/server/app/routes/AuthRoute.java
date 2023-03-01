package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.handlers.HomeHandler;
import org.server.app.handlers.LoginHandler;
import org.server.app.utils.HandlerBuilder;

public class AuthRoute {
    private final HttpServer server;
    private final String basePath;

    public AuthRoute(HttpServer server, String basePath) {
        this.server = server;
        this.basePath = basePath;
    }

    public void registerRoutes() {
        HttpHandler loginHandler = new HandlerBuilder(new LoginHandler()).build();
        this.server.createContext(basePath + "/login", loginHandler);
    }
}
