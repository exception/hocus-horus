package com.joinhocus.horus.twitter;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.twitter.data.TwitterUser;
import com.joinhocus.horus.twitter.oauth.TwitterOauth;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import java.util.concurrent.CompletableFuture;

public class TwitterAPI {

    public static final String CONSUMER_KEY = "<redacted>";
    public static final String CONSUMER_SECRET = "<redacted>";
    public static final String ACCESS_TOKEN = "<redacted>";
    public static final String ACCESS_TOKEN_SECRET = "<redacted>";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static final TwitterOauth OAUTH = new TwitterOauth();

    public static final TwitterOAuthHeaderGenerator HEADER_GENERATOR = new TwitterOAuthHeaderGenerator(
            CONSUMER_KEY,
            CONSUMER_SECRET,
            ACCESS_TOKEN,
            ACCESS_TOKEN_SECRET
    );

    public static CompletableFuture<TwitterUser> fetchUserInfo(String userId) {
        String url = "https://api.twitter.com/1.1/users/lookup.json";
        return Unirest.get(url)
                .header("Authorization", HEADER_GENERATOR.generateHeader(
                        "GET",
                        url,
                        ImmutableMap
                                .<String, String>builder()
                                .put("user_id", userId)
                                .build()
                ))
                .queryString("user_id", userId)
                .asJsonAsync()
                .thenCompose(response -> {
                    if (!response.isSuccess()) {
                        return CompletableFutures.failedFuture(new IllegalStateException(response.getBody().toString()));
                    }

                    JsonNode node = response.getBody();
                    JsonElement element = GSON.fromJson(node.toString(), JsonElement.class);
                    JsonArray array = element.getAsJsonArray();
                    JsonElement user = array.get(0);
                    return CompletableFuture.completedFuture(
                            GSON.fromJson(user, TwitterUser.class)
                    );
                });
    }

    public static CompletableFuture<TwitterUser> searchUser(String userName) {
        String url = "https://api.twitter.com/1.1/users/lookup.json";
        return Unirest.get(url)
                .header("Authorization", HEADER_GENERATOR.generateHeader(
                        "GET",
                        url,
                        ImmutableMap
                                .<String, String>builder()
                                .put("screen_name", userName)
                                .build()
                ))
                .queryString("screen_name", userName)
                .asJsonAsync()
                .thenCompose(response -> {
                    if (!response.isSuccess()) {
                        return CompletableFutures.failedFuture(new IllegalStateException(response.getBody().toString()));
                    }

                    JsonNode node = response.getBody();
                    JsonElement element = GSON.fromJson(node.toString(), JsonElement.class);
                    JsonArray array = element.getAsJsonArray();
                    JsonElement user = array.get(0);
                    return CompletableFuture.completedFuture(
                            GSON.fromJson(user, TwitterUser.class)
                    );
                });
    }

}
