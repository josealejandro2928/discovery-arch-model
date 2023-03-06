package org.server.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.morphia.Datastore;
import org.discover.arch.model.Config;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.MongoDbConnection;
import org.server.app.data.UserModel;
import org.server.app.utils.CustomMapMapper;


import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HomeController {
    private static final ObjectMapper objectMapper = CustomMapMapper.getInstance();

    public HttpHandler homeHandler = (HttpExchange exchange) -> {
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    };
    public HttpHandler invalidCacheHandler = (HttpExchange exchange) -> {
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        File file = new File(Paths.get(configUser.getRootPath(), ".cache.txt").toString());
        if (file.exists()) {
            Config.deleteDirectory(Paths.get(configUser.getRootPath(), ".cache.txt"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Ok");
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    };
}
