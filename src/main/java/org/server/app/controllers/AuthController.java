package org.server.app.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class AuthController {
    private static final CustomMapMapper objectMapper = CustomMapMapper.getInstance();


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
            MongoDbConnection mongoDbConnection = MongoDbConnection.getInstance();
            Datastore datastore = mongoDbConnection.datastore;
            UserModel user = datastore.find(UserModel.class).filter(Filters.eq("email", body.get("email"))).first();

            if (user == null) throw new ServerError(401, "invalid email");
            try {
                if (!user.verifyPassword((String) body.get("password")))
                    throw new ServerError(401, "Invalid password");
            } catch (NoSuchAlgorithmException e) {
                throw new ServerError(401, e.getMessage());
            }

            String jwt = AuthController.createJWT(user.getId(), 1000 * 60 * 60);
            String token = String.format("Bearer %s", jwt);
            Map<String, Object> userToClient = objectMapper.convertValue(user, new TypeReference<>() {
            });
            userToClient.remove("password");
            response.put("user", userToClient);
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
            ConfigUserModel configUserModel = AuthController.initUserDirectory(userModel);
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

    public static ConfigUserModel initUserDirectory(UserModel userModel) throws IOException {
        MongoDbConnection mongoDbConnection = MongoDbConnection.getInstance();
        Datastore datastore = mongoDbConnection.datastore;
        ConfigServer configServer = ConfigServer.getInstance();
        Path directoryPath = Paths.get(Objects.requireNonNull(configServer.dotenv.get("ROOT_STORAGE")), userModel.getEmail()).toAbsolutePath();
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

    public static String createJWT(String userId, long ttlMillis) {
        ConfigServer configServer = ConfigServer.getInstance();
        long nowMillis = System.currentTimeMillis();
        String secretKey = configServer.dotenv.get("SECRET_KEY");
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        Map<String, Object> payloadClaims = new HashMap<>();
        payloadClaims.put("userId", userId);

        long expMillis = nowMillis + ttlMillis;
        Date exp = new Date(expMillis);

        return JWT.create()
                .withIssuer("auth0")
                .withSubject("JWT token")
                .withExpiresAt(exp)
                .withClaim("payload", payloadClaims)
                .sign(algorithm);
    }

}
