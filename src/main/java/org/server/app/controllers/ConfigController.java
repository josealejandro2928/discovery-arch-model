package org.server.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.UserModel;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.ServerError;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ConfigController {
    private static final ObjectMapper objectMapper = CustomMapMapper.getInstance();

    public HttpHandler configHandler = (HttpExchange exchange) -> {
        String method = exchange.getRequestMethod();
        if (method.equals("GET")) {
            getConfig(exchange);
        } else if (method.equals("POST")) {
            editConfig(exchange);
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }
    };

    void getConfig(HttpExchange exchange) throws IOException {
        UserModel loggedUser = (UserModel) exchange.getAttribute("loggedUser");
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        Map<String, Object> data = new HashMap<>();
        data.put("config", configUser);
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    void editConfig(HttpExchange exchange) throws IOException {

    }
}
