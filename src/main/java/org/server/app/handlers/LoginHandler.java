package org.server.app.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.ServerError;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LoginHandler implements HttpHandler {
    private final CustomMapMapper objectMapper = new CustomMapMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toString();
        if (method.equals("POST")) {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> body = objectMapper.getBodyAsMap(requestBody);
            Map<String, Object> dataResponse = this.login(exchange, body);
            String response = objectMapper.writeValueAsString(dataResponse);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }

    }

    private Map<String, Object> login(HttpExchange exchange, Map<String, Object> body) throws ServerError {
        Map<String, Object> response = new HashMap<>();
        List<String> errors = new ArrayList<>();
        if (!body.containsKey("email")) errors.add("email must be provided");
        if (!body.containsKey("password")) errors.add("password must be provided");
        if (errors.size() > 0)
            throw new ServerError(401, "Invalid user authentication", Collections.singletonList(errors));
        String token = String.format("Bearer: %s", body.get("email"));
        response.put("username", body.get("email"));
        response.put("token", token);
        return response;
    }
}
