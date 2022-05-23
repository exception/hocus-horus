package com.joinhocus.horus.http.routes.entity;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.FollowsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.MongoIds;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GetFollowsEntityHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String id = context.queryParam("id");
        if (Strings.isNullOrEmpty(id)) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("id cannot be empty"));
        }
        ObjectId mongoId = MongoIds.parseId(id);
        if (mongoId == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("malformed id"));
        }

        return MongoDatabase.getInstance().getRepo(FollowsRepo.class).follows(account, mongoId).thenApply(follows -> {
            return Response.of(Response.Type.OKAY).append("follows", follows);
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
