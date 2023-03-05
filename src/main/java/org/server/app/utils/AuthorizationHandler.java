package org.server.app.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.morphia.Datastore;
import dev.morphia.query.experimental.filters.Filters;
import org.bson.types.ObjectId;
import org.server.app.data.ConfigUserModel;
import org.server.app.data.MongoDbConnection;
import org.server.app.data.UserModel;

import java.io.IOException;

public class AuthorizationHandler extends HandlerMiddleware {
    private HttpHandler innerHandler;

    public AuthorizationHandler(HttpHandler handler) {
        this.innerHandler = handler;
    }

    public AuthorizationHandler() {
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Datastore datastore = MongoDbConnection.getInstance().datastore;
        ConfigServer configServer = ConfigServer.getInstance();
        String secretKey = configServer.dotenv.get("SECRET_KEY");
        if (!exchange.getRequestHeaders().containsKey("Authorization"))
            throw new ServerError(401, "Authorization must be provided");
        String authorizationHeader = exchange.getRequestHeaders().get("Authorization").get(0);

        String token = authorizationHeader.split("Bearer ")[1];
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm).withIssuer("auth0").build();
        DecodedJWT jwt;
        String userId;
        try {
            jwt = verifier.verify(token);
            userId = jwt.getClaim("payload").asMap().get("userId").toString();
        } catch (Exception e) {
            throw new ServerError(401, e.getMessage());
        }

        UserModel user = datastore.find(UserModel.class).filter(Filters.eq("_id", new ObjectId(userId))).first();
        if (user == null) throw new ServerError(401, "Invalid user credentials, invalid token");
        exchange.setAttribute("loggedUser", user);
        ConfigUserModel configUser = datastore.find(ConfigUserModel.class).filter(Filters.eq("user", user)).first();
        exchange.setAttribute("configUser", configUser);
        this.innerHandler.handle(exchange);


    }

    @Override
    public void setInnerHandler(HttpHandler innerHandler) {
        this.innerHandler = innerHandler;
    }
}
