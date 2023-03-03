package org.server.app.routes;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.server.app.controllers.HomeController;
import org.server.app.controllers.ModelRepositoryController;
import org.server.app.controllers.ReportsController;
import org.server.app.utils.AuthorizationHandler;
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

        HttpHandler listModelsHandler = new HandlerBuilder(modelsController.listModelsHandler)
                .setAuthorizationHandler(new AuthorizationHandler()).build();

        HttpHandler discoverModelsHandler = new HandlerBuilder(modelsController.discoverModelHandler)
                .setAuthorizationHandler(new AuthorizationHandler()).build();

        HttpHandler analyseModelsHandler = new HandlerBuilder(modelsController.analyseModelsHandler)
                .setAuthorizationHandler(new AuthorizationHandler()).build();

        HttpHandler reportHandler = new HandlerBuilder(reportsController.reportHandler)
                .setAuthorizationHandler(new AuthorizationHandler()).build();

        this.server.createContext(basePath + "/home", homeHandler);
        this.server.createContext(basePath + "/reports", reportHandler);

        this.server.createContext(basePath + "/models/discover", discoverModelsHandler);
        this.server.createContext(basePath + "/models/analyse", analyseModelsHandler);
        this.server.createContext(basePath + "/models", listModelsHandler);
    }
}
