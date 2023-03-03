package org.server.app.utils;

import com.sun.net.httpserver.HttpHandler;

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
        return new LoggingHandler(errorHandler);
    }
}
