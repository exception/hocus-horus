package com.joinhocus.horus.http.routes.waitlist.register.twitter;

import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.Environment;
import com.joinhocus.horus.twitter.TwitterAPI;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class RegisterWithTwitterHandler implements DefinedTypesHandler<EmptyRequest> {

    private final String CALLBACK_URL = Environment.current().getUrl() + "/flow/twitter";
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        return TwitterAPI.OAUTH.getRequestToken(CALLBACK_URL).thenApply(res -> {
            return Response.of(Response.Type.OKAY).append("token", res.getToken());
        }).exceptionally(err -> {
            logger.error("", err);
            throw new InternalServerErrorResponse();
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
