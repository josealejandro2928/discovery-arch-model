package org.server.app.data;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;

import java.net.UnknownHostException;

public class MongoDbConnection {
    static MongoDbConnection instance = null;
    static public String URI = "localhost";
    static public int PORT = 27017;
    static String DB_NAME = "server_thesis_db";
    public MongoClient mongo;
    public MongoDatabase db;
    public Datastore datastore;

    private MongoDbConnection(MongoClient mongo, MongoDatabase db) {
        this.db = db;
        this.mongo = mongo;
        this.datastore = Morphia.createDatastore(mongo, db.getName());
        datastore.getMapper().mapPackage("com.mongodb.morphia.entities");
        datastore.ensureIndexes();
    }

    public static MongoDbConnection getInstance() throws UnknownHostException {
        if (instance != null) return instance;
        instance = loadInstance();
        return instance;
    }

    static MongoDbConnection loadInstance() {
        MongoClient mongo = MongoClients.create(String.format("mongodb://%s:%s", URI, PORT));
        MongoDatabase db = mongo.getDatabase(DB_NAME);
        instance = new MongoDbConnection(mongo, db);
        return instance;
    }
}
