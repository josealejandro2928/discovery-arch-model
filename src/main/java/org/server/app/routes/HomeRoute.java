package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.handlers.HomeHandler;
import org.server.app.handlers.ModelRepositoryHandler;
import org.server.app.handlers.ReportsHandler;
import org.server.app.utils.AuthorizationHandler;
import org.server.app.utils.HandlerBuilder;
import org.server.app.utils.HandlerMiddleware;

public class HomeRoute {
    private final HttpServer server;
    private final String basePath;

    public HomeRoute(HttpServer server, String basePath) {
        this.server = server;
        this.basePath = basePath;
    }

    public void registerRoutes() {
        HttpHandler homeHandler = new HandlerBuilder(new HomeHandler()).build();
        ModelRepositoryHandler modelRepositoryHandler = new ModelRepositoryHandler();

        HttpHandler listModelsHandler = new HandlerBuilder(modelRepositoryHandler.listModelsHandler)
                .setAuthorizationHandler(new AuthorizationHandler()).build();

        HttpHandler discoverModelHandler = new HandlerBuilder(modelRepositoryHandler.discoverModelHandler)
                .setAuthorizationHandler(new AuthorizationHandler()).build();

        HttpHandler reportHandler = new HandlerBuilder(new ReportsHandler().reportHandler)
                .setAuthorizationHandler(new AuthorizationHandler()).build();

        this.server.createContext(basePath + "/home", homeHandler);
        this.server.createContext(basePath + "/reports", reportHandler);

        this.server.createContext(basePath + "/models/discover", discoverModelHandler);
        this.server.createContext(basePath + "/models", listModelsHandler);
    }
}
