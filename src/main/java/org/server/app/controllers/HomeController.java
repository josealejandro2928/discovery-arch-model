package org.server.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.morphia.Datastore;
import org.discover.arch.model.Config;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.MongoDbConnection;
import org.server.app.data.UserModel;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.JupyterServersController;
import org.server.app.utils.ServerError;
import org.server.app.utils.URIUtils;


import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public HttpHandler invalidCacheHandler = (HttpExchange exchange) -> {
        ConfigUserModel configUser = (ConfigUserModel) exchange.getAttribute("configUser");
        File file = new File(Paths.get(configUser.getRootPath(), ".cache.txt").toString());
        if (file.exists()) {
            Config.deleteDirectory(Paths.get(configUser.getRootPath(), ".cache.txt"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Ok");
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    };
    public HttpHandler startJupyter = (HttpExchange exchange) -> {
        UserModel loggedUser = (UserModel) exchange.getAttribute("loggedUser");
        ConfigUserModel configUserModel = (ConfigUserModel) exchange.getAttribute("configUser");
        JupyterServersController jupyterServersCtrl = JupyterServersController.getInstance();
        Map<String, String> data = jupyterServersCtrl.startNewSession(loggedUser, configUserModel);
        data.put("message", "Ok");
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    };
    public HttpHandler stopJupyter = (HttpExchange exchange) -> {
        UserModel loggedUser = (UserModel) exchange.getAttribute("loggedUser");
        JupyterServersController jupyterServersCtrl = JupyterServersController.getInstance();
        jupyterServersCtrl.stopSession(loggedUser);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Ok");
        String response = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    };

    public HttpHandler downloadFile = (HttpExchange exchange) -> {
        ConfigUserModel configUserModel = (ConfigUserModel) exchange.getAttribute("configUser");
        Map<String, String> query = URIUtils.getQuery(exchange.getRequestURI());
        if (!query.containsKey("file"))
            throw new ServerError(400, "You must pass the file");
        Path rootPath = Paths.get(configUserModel.getRootPath());
        Path pathToFile;
        File file;

        switch (query.get("file")) {
            case "results":
                pathToFile = rootPath.resolve("results.csv");
                file = new File(pathToFile.toAbsolutePath().toString());
                exchange.getResponseHeaders().add("Content-Type", "text/csv");
                exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + "results.csv");
                break;
            case "legends":
                pathToFile = rootPath.resolve("legends.csv");
                file = new File(pathToFile.toAbsolutePath().toString());
                exchange.getResponseHeaders().add("Content-Type", "text/csv");
                exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + "legends.csv");
                break;
            case "conv-logs":
                pathToFile = Paths.get(rootPath.toString(),configUserModel.getOutputFolderName(),"conversion-logs.json");
                file = new File(pathToFile.toAbsolutePath().toString());
                exchange.getResponseHeaders().add("Content-Type", "text/csv");
                exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + "conversion-logs.json");
                break;
            default:
                throw new ServerError(404, "not found");
        }

        exchange.sendResponseHeaders(200, file.length());
        OutputStream outputStream = exchange.getResponseBody();
        Files.copy(file.toPath(), outputStream);
        outputStream.close();
    };
}
