package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.controllers.HomeController;
import org.server.app.controllers.ModelRepositoryController;
import org.server.app.controllers.ReportsController;
import org.server.app.utils.AuthorizationHandler;
import org.server.app.utils.FormDataHandler;
import org.server.app.utils.HandlerBuilder;

public class HomeRoute {
    private final HttpServer server;
    private final String basePath;

    public HomeRoute(HttpServer server, String basePath) {
        this.server = server;
        this.basePath = basePath;
    }

    public void registerRoutes() {
        HomeController homeController = new HomeController();
        ModelRepositoryController modelsController = new ModelRepositoryController();
        ReportsController reportsController = new ReportsController();

        HttpHandler homeHandler = new HandlerBuilder(homeController.homeHandler).build();

        HttpHandler modelsHandler = new HandlerBuilder(modelsController.modelsHandler)
                .setMiddlewareHandler(new FormDataHandler())
                .setMiddlewareHandler(new AuthorizationHandler()).build();

        HttpHandler discoverModelsHandler = new HandlerBuilder(modelsController.discoverModelHandler)
                .setMiddlewareHandler(new AuthorizationHandler()).build();

        HttpHandler analyseModelsHandler = new HandlerBuilder(modelsController.analyseModelsHandler)
                .setMiddlewareHandler(new AuthorizationHandler()).build();

        HttpHandler reportHandler = new HandlerBuilder(reportsController.reportHandler)
                .setMiddlewareHandler(new AuthorizationHandler()).build();

        this.server.createContext(basePath + "/home", homeHandler);
        this.server.createContext(basePath + "/reports", reportHandler);

        this.server.createContext(basePath + "/models/discover", discoverModelsHandler);
        this.server.createContext(basePath + "/models/analyse", analyseModelsHandler);
        this.server.createContext(basePath + "/models", modelsHandler);
    }
}
