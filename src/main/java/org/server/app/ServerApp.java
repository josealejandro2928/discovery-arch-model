package org.server.app;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.server.app.handlers.NotFoundHandler;
import org.server.app.routes.AuthRoute;
import org.server.app.routes.HomeRoute;
import org.server.app.utils.HandlerBuilder;

import java.io.IOException;

public class ServerApp {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", new HandlerBuilder(new NotFoundHandler()).build());
        // Registering Routes ////////////////////////////
        HomeRoute homeRoute = new HomeRoute(server, "");
        AuthRoute authRoute = new AuthRoute(server, "/auth");
        homeRoute.registerRoutes();
        authRoute.registerRoutes();
        System.out.println("Registered routes successfully");
        /////////////////////////////////////////////////
        server.start();
        System.out.println("Server started on port 8000");
    }
}
