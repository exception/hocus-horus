package com.joinhocus.horus.http.routes.entity;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.HandlesUtil;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class SearchByHandleHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String query = context.queryParam("query");
        if (Strings.isNullOrEmpty(query)) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("query cannot be empty"));
        }

        return HandlesUtil.getHandlesByQuery(query).thenApply(list -> {
            JsonArray array = new JsonArray();
            for (JsonObject object : list) {
                array.add(object);
            }

            return Response.of(Response.Type.OKAY).append("results", array);
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
