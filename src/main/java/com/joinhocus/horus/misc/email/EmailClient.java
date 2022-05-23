package com.joinhocus.horus.misc.email;

import com.wildbit.java.postmark.Postmark;
import com.wildbit.java.postmark.client.ApiClient;

public class EmailClient {

    private final String API_TOKEN = "<redacted>";
    private static EmailClient instance;
    private final ApiClient apiClient;

    public EmailClient() {
        this.apiClient = Postmark.getApiClient(API_TOKEN);
    }

    public ApiClient getClient() {
        return apiClient;
    }

    public static EmailClient getInstance() {
        if (instance == null) {
            instance = new EmailClient();
        }

        return instance;
    }
}
