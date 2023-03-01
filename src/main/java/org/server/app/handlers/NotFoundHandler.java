package org.server.app.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.ServerError;

import java.io.IOException;
import java.io.OutputStream;

public class NotFoundHandler implements HttpHandler {
    private final ObjectMapper objectMapper = new CustomMapMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ServerError data = new ServerError(404, "Route with the url: " + exchange.getRequestURI() + " is not Found", null);
        String response = objectMapper.writeValueAsString(data.toMap());
        exchange.getResponseHeaders().add("Content-Type","application/json");
        exchange.sendResponseHeaders(404, response.length());

        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
