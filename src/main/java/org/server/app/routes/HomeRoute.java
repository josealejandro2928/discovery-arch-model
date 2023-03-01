package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.handlers.HomeHandler;
import org.server.app.utils.HandlerBuilder;

public class HomeRoute {
    private final HttpServer server;
    private final String basePath;

    public HomeRoute(HttpServer server, String basePath) {
        this.server = server;
        this.basePath = basePath;
    }

    public void registerRoutes() {
        HttpHandler homeHandler = new HandlerBuilder(new HomeHandler()).build();
        this.server.createContext(basePath + "/home", homeHandler);
    }
}
