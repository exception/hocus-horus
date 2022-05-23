package com.joinhocus.horus.twitter.oauth;

import com.google.common.collect.ImmutableMap;
import com.joinhocus.horus.twitter.TwitterAPI;
import com.joinhocus.horus.twitter.TwitterOAuthHeaderGenerator;
import kong.unirest.Unirest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TwitterOauth {

    private final String OAUTH_URL = "https://api.twitter.com/oauth/";

    public CompletableFuture<OAuthResponseValues> getRequestToken(String callbackUrl) {
        String url = OAUTH_URL + "request_token";
        return Unirest.post(url)
                .header("Authorization", TwitterAPI.HEADER_GENERATOR.generateHeader(
                        "POST",
                        url,
                        ImmutableMap
                                .<String, String>builder()
                                .put("oauth_callback", callbackUrl)
                                .put("x_auth_access_type", "read")
                                .build()
                ))
                .queryString("oauth_callback", callbackUrl)
                .queryString("x_auth_access_type", "read")
                .asStringAsync()
                .thenApply(response -> {
                    if (!response.isSuccess()) {
                        throw new IllegalStateException("Failed to request token, " + response.getBody());
                    }

                    String strRes = response.getBody();
                    Map<String, String> res = parseResponse(strRes);
                    return new OAuthResponseValues(
                            res.getOrDefault("oauth_token", ""),
                            res.getOrDefault("oauth_token_secret", "")
                    );
                });
    }

    public CompletableFuture<OAuthAccountResponse> getOauthToken(String token, String verifier) {
        TwitterOAuthHeaderGenerator generator = new TwitterOAuthHeaderGenerator(
                TwitterAPI.CONSUMER_KEY,
                TwitterAPI.CONSUMER_SECRET,
                token,
                TwitterAPI.ACCESS_TOKEN_SECRET
        );
        String url = OAUTH_URL + "access_token";
        return Unirest.post(url)
                .queryString("oauth_verifier", verifier)
                .header("Authorization", generator.generateHeader(
                        "POST",
                        url,
                        Collections.emptyMap()
                ))
                .asStringAsync()
                .thenApply(response -> {
                    if (!response.isSuccess()) {
                        throw new IllegalStateException("Failed to validate tokens: " + response.getBody());
                    }

                    String strRes = response.getBody();
                    Map<String, String> res = parseResponse(strRes);
                    return new OAuthAccountResponse(
                            res.getOrDefault("user_id", null),
                            res.getOrDefault("screen_name", null)
                    );
                });
    }

    private Map<String, String> parseResponse(String response) {
        String[] split = response.split("&");
        Map<String, String> res = new HashMap<>(3);
        for (String val : split) {
            String key = val.substring(0, val.indexOf('='));
            String actualVal = val.substring(val.indexOf('=') + 1);

            res.put(key, actualVal);
        }
        return res;
    }

}
