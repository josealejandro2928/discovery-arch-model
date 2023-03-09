package org.server.app.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class HandlerBuilder {
    HttpHandler httpHandler;

    public HandlerBuilder(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    public HandlerBuilder setMiddlewareHandler(HandlerMiddleware middHandler) {
        middHandler.setInnerHandler(this.httpHandler);
        this.httpHandler = middHandler;
        return this;
    }

    public HttpHandler build() {
        HttpHandler errorHandler = new ErrorHandler(httpHandler);
        HttpHandler handler = new LoggingHandler(errorHandler);
        handler = new CorsHandler(handler);
        return handler;
    }


}
