package org.server.app.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import org.server.app.utils.CustomMapMapper;


import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.server.app.utils.ServerError;

public class HomeHandler implements HttpHandler {
    private final ObjectMapper objectMapper = new CustomMapMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println(System.getenv("ROOT_STORAGE"));
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
