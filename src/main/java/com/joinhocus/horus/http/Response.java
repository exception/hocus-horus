package com.joinhocus.horus.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;

@Getter
public class Response {

    private final int code;
    private String message;
    private final boolean success;

    private final JsonObject object;

    private Response(Type type) {
        this.code = type.code;
        this.message = type.message;
        this.success = type.success;

        this.object = new JsonObject();
        this.object.addProperty("status_code", this.code);
        this.object.addProperty("success", this.success);
        this.object.addProperty("message", this.message);
    }

    public static Response of(Type type) {
        return new Response(type);
    }

    public Response append(String key, String value) {
        this.object.addProperty(key, value);
        return this;
    }

    public Response append(String key, Number value) {
        this.object.addProperty(key, value);
        return this;
    }

    public Response append(String key, boolean value) {
        this.object.addProperty(key, value);
        return this;
    }

    public Response append(String key, JsonElement value) {
        this.object.add(key, value);
        return this;
    }

    public Response setMessage(String message) {
        this.message = message;
        this.object.addProperty("message", message);
        return this;
    }

    public JsonObject toJSON() {
        return this.object;
    }

    public enum Type {
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        RATE_LIMIT(420, "Too many requests. Rate limit exceeded"),
        BAD_REQUEST(400, "Bad Request"),
        INTERNAL_SERVER_EXCEPTION(500, "Internal server error"),
        OKAY(200, "Successful", true),
        OKAY_CREATED(201, "Successful", true),

        BETA_FEATURE(418, "This is a beta feature"),
        FEATURE_DISABLED(419, "This feature is temporarily disabled");

        int code;
        String message;
        boolean success;

        Type(int code, String message, boolean success) {
            this.code = code;
            this.message = message;
            this.success = success;
        }

        Type(int code, String message) {
            this(code, message, false);
        }

        public int getStatusCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

}
