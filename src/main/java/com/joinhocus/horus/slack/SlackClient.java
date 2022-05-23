package com.joinhocus.horus.slack;

import com.google.common.collect.Lists;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.request.RequestHeaders;
import com.slack.api.bolt.util.SlackRequestParser;
import com.slack.api.methods.MethodsClient;
import io.javalin.http.Context;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlackClient {

    private static SlackClient instance;
    private MethodsClient client;
    public static final String XOXB_TOKEN = "<redacted>";
    @SuppressWarnings("FieldCanBeLocal")
    private final String SIGNING_SECRET = "<redacted>";

    private final App app;
    private final SlackRequestParser parser;

    public SlackClient() {
        try {
            SlackConfig config = SlackConfig.class.newInstance();
            Field field = SlackConfig.class.getDeclaredField("httpClientResponseHandlers");
            field.setAccessible(true);
            //noinspection rawtypes
            field.set(config, new ArrayList());
            this.client = Slack.getInstance(config).methods(XOXB_TOKEN);
        } catch (Exception e) {
            e.printStackTrace();
            this.client = Slack.getInstance().methods(XOXB_TOKEN);
        }

        AppConfig appConfig = AppConfig.builder()
                .signingSecret(SIGNING_SECRET)
                .singleTeamBotToken(XOXB_TOKEN)
                .build();

        this.app = new App(appConfig);
        this.parser = new SlackRequestParser(appConfig);
        this.app.start();
    }

    public MethodsClient getClient() {
        return client;
    }

    public static SlackClient getInstance() {
        if (instance == null) {
            instance = new SlackClient();
        }

        return instance;
    }

    public App getApp() {
        return app;
    }

    public Request<?> transform(Context context) {
        String body = context.body();
        SlackRequestParser.HttpRequest rawRequest = SlackRequestParser.HttpRequest.builder()
                .requestUri(context.url())
                .queryString(context.queryParamMap())
                .requestBody(body)
                .remoteAddress(context.ip())
                .headers(new RequestHeaders(toSlackMap(context.headerMap())))
                .build();
        return parser.parse(rawRequest);
    }

    private Map<String, List<String>> toSlackMap(Map<String, String> headers) {
        Map<String, List<String>> slackMap = new HashMap<>();
        headers.forEach((key, value) -> slackMap.put(key, Lists.newArrayList(value.split(","))));
        return slackMap;
    }

    public void registerExtension(SlackClientExtension extension) {
        extension.register(this.app);
    }
}
