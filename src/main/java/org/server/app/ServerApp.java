package org.server.app;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import org.server.app.data.MongoDbConnection;
import org.server.app.routes.ConfigRoute;
import org.server.app.utils.NotFoundHandler;
import org.server.app.routes.AuthRoute;
import org.server.app.routes.HomeRoute;
import org.server.app.utils.ConfigServer;
import org.server.app.utils.HandlerBuilder;

public class ServerApp {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", new HandlerBuilder(new NotFoundHandler()).build());
        /// Loading the .env configurations ///
        System.out.println("Loading env configurations");
        ConfigServer.getInstance();
        // Connection with MongoDb //////////////////////
        MongoDbConnection mongoDbConnection = MongoDbConnection.getInstance();
        System.out.println("Connection to mongo Db successfully: " + mongoDbConnection);
        // Registering Routes ////////////////////////////
        HomeRoute homeRoute = new HomeRoute(server, "");
        AuthRoute authRoute = new AuthRoute(server, "/auth");
        ConfigRoute configRoute = new ConfigRoute(server, "/config");
        homeRoute.registerRoutes();
        authRoute.registerRoutes();
        configRoute.registerRoutes();
        System.out.println("Registered routes successfully");
        /////////////////////////////////////////////////
        server.start();
        System.out.println("Server started on port 8000");
    }
}
