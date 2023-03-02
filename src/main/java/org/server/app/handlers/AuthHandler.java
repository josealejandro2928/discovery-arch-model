package org.server.app.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpHandler;
import dev.morphia.Datastore;
import dev.morphia.query.experimental.filters.Filters;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.MongoDbConnection;
import org.server.app.data.UserModel;
import org.server.app.dto.SignInRequest;
import org.server.app.utils.ConfigServer;
import org.server.app.utils.CustomMapMapper;
import org.server.app.utils.ServerError;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class AuthHandler {
    private final CustomMapMapper objectMapper = new CustomMapMapper();


    public HttpHandler loginHandler = exchange -> {
        String method = exchange.getRequestMethod();
        if (method.equals("POST")) {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> body = objectMapper.getBodyAsMap(requestBody);
            Map<String, Object> response = new HashMap<>();
            List<String> errors = new ArrayList<>();
            if (!body.containsKey("email")) errors.add("email must be provided");
            if (!body.containsKey("password")) errors.add("password must be provided");
            if (errors.size() > 0)
                throw new ServerError(401, "Invalid user authentication", Collections.singletonList(errors));
            String token = String.format("Bearer: %s", body.get("email"));
            response.put("username", body.get("email"));
            response.put("token", token);

            String responseStr = objectMapper.writeValueAsString(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseStr.length());
            OutputStream os = exchange.getResponseBody();
            os.write(responseStr.getBytes());
            os.close();
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }
    };
    public HttpHandler signInHandler = exchange -> {
        String method = exchange.getRequestMethod();
        if (method.equals("POST")) {
            MongoDbConnection mongoDbConnection = MongoDbConnection.getInstance();
            Datastore datastore = mongoDbConnection.datastore;
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            SignInRequest body = objectMapper.readValue(requestBody, SignInRequest.class);
            UserModel userModel = datastore.find(UserModel.class).filter(Filters.eq("email", body.getEmail())).first();
            if (userModel != null) {
                throw new ServerError(400, "User with email: " + userModel.getEmail() + " already exists", null);
            }
            if (!body.getPassword().equals(body.getConfirmPassword())) {
                throw new ServerError(400, "Passwords must be equal", null);
            }

            try {
                userModel = new UserModel(body.getName(), body.getLastName(), body.getEmail(), body.getPassword());
                userModel = datastore.save(userModel);
            } catch (NoSuchAlgorithmException e) {
                throw new ServerError(500, e.getMessage());
            }
            ConfigUserModel configUserModel =  AuthHandler.initUserDirectory(userModel);
            Map<String, Object> data = new HashMap<>();
            data.put("user", userModel);
            data.put("config", configUserModel);
            data.put("message", "User registered");
            String response = objectMapper.writeValueAsString(data);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            throw new ServerError(405, "Not Allowed for /login", null);
        }
    };

    static ConfigUserModel initUserDirectory(UserModel userModel) throws IOException {
        CustomMapMapper objectMapper = new CustomMapMapper();
        MongoDbConnection mongoDbConnection = MongoDbConnection.getInstance();
        Datastore datastore = mongoDbConnection.datastore;
        ConfigServer configServer = ConfigServer.getInstance();
        Path directoryPath = Paths.get(configServer.dotenv.get("ROOT_STORAGE"), userModel.getEmail()).toAbsolutePath();
        File file = new File(directoryPath.toString());
        ConfigUserModel configUserModel;
        if (file.exists()) {
            configUserModel = datastore.find(ConfigUserModel.class).filter(Filters.eq("user", userModel.getId())).first();
            if (configUserModel == null) {
                configUserModel = ConfigUserModel.buildConfig(userModel);
                configUserModel = datastore.save(configUserModel);
            }
            return configUserModel;
        }
        file.mkdir();
        Paths.get(directoryPath.toString(), "local_models").toFile().mkdir();
        Paths.get(directoryPath.toString(), "github").toFile().mkdir();
        configUserModel = ConfigUserModel.buildConfig(userModel);
        configUserModel = datastore.save(configUserModel);
        String configJsonStr = objectMapper.writeValueAsString(configUserModel);
        FileWriter fw = new FileWriter(Paths.get(configUserModel.getPathToConfigJson()).toString());
        fw.write(configJsonStr);
        fw.close();
        return configUserModel;
    }

}
