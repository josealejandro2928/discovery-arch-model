package org.server.app.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class HandlerBuilder {
    HttpHandler httpHandler;
    public HandlerBuilder(HttpHandler httpHandler){
        this.httpHandler = httpHandler;
    }
    public HttpHandler build(){
        HttpHandler errorHandler = new ErrorHandler(httpHandler);
        return new LoggingHandler(errorHandler);
    }
}
