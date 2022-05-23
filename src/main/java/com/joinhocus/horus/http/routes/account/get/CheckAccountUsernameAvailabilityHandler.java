package com.joinhocus.horus.http.routes.account.get;

import com.google.common.base.Strings;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.HandlesUtil;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class CheckAccountUsernameAvailabilityHandler implements DefinedTypesHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String name = context.queryParam("username");
        if (Strings.isNullOrEmpty(name)) {
            return wrap(Response.of(Response.Type.BAD_REQUEST));
        }
        return HandlesUtil.isHandleAvailable(name).thenApply(available -> {
            return Response.of(Response.Type.OKAY).append("available", available);
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
