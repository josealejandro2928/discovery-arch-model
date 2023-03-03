package org.server.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.server.app.utils.CustomMapMapper;


import java.io.OutputStream;
import java.io.IOException;
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
}
