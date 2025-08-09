package com.minecraftcivilizations.specialization.Data;

import com.minecraftcivilizations.specialization.Analytics.AnalyticsData;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

public class MongoConnection {


    public static MongoClient mongoClient;

    public static void startDBConnection() {
        String connectionString = "mongodb://skibidi:bongo123@wiki.civlabs.org:27017";
        mongoClient = MongoClients.create(connectionString);
    }

    public static <T> JacksonMongoCollection<T> getCollection(Collections<T> type) {
        return JacksonMongoCollection.builder().build(mongoClient.getDatabase("main"), type.name, type.type, UuidRepresentation.STANDARD);
    }

    public static class Collections<T> {

        String name;
        Class<T> type;

        public Collections(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public static Collections<AnalyticsData> ANALYTICS = new Collections<>("analytics", AnalyticsData.class);
    }
}
