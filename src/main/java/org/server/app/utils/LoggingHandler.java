package org.server.app.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class LoggingHandler implements HttpHandler {
    private final HttpHandler handler;

    public LoggingHandler(HttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        String requestURI = exchange.getRequestURI().toString();
        long startTime = System.nanoTime();
        // Call actual handler
        handler.handle(exchange);
        long endTime = System.nanoTime();
        double elapsedTime = (double) (endTime - startTime) / 1000000;
        int responseCode = exchange.getResponseCode();
        String contentLength = exchange.getResponseHeaders().get("Content-Length").toString();
        System.out.println(String.format("\u001B[33m Info:\u001B[0m %s %s %s %s ms - res:%s \u001B[0m",
                requestMethod, requestURI, this.getStatusCode(responseCode),(int)elapsedTime,contentLength));

    }

    String getStatusCode(int code) {
        if (code > 100 && code < 300)
            return String.format("\u001B[32m%s\u001B[0m", code);
        if (code >= 300)
            return String.format("\u001B[31m%s\u001B[0m", code);
        return code + "";
    }
}
