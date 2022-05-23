package com.joinhocus.horus.http.routes.twitter.info;

import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.twitter.TwitterAPI;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class TwitterInfoHandler implements DefinedTypesHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String twitterId = context.pathParam("twitterId");
        return TwitterAPI.fetchUserInfo(twitterId).thenApply(user -> {
            return Response.of(Response.Type.OKAY)
                    .append("handle", user.getHandle())
                    .append("name", user.getName());
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
