package org.server.app.utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public class ErrorHandler implements HttpHandler {

    private final HttpHandler handler;
    private static final ObjectMapper objectMapper = CustomMapMapper.getInstance();

    public ErrorHandler(HttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            handler.handle(exchange);
        } catch (Exception e){
            System.err.println(e);
            e.printStackTrace();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            OutputStream os = exchange.getResponseBody();
            ServerError error = new ServerError(e);
            String response = objectMapper.writeValueAsString(error.toMap());
            exchange.sendResponseHeaders(error.code, response.length());
            os.write(response.getBytes());
            os.close();
        }
    }
}
