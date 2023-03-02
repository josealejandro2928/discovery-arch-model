package org.server.app.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public abstract class HandlerMiddleware implements HttpHandler {
    private HttpHandler innerHandler;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
    }

    public void setInnerHandler(HttpHandler handler) {

    }
}
