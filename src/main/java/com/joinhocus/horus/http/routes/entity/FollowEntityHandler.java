package com.joinhocus.horus.http.routes.entity;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.activity.ActivityNotification;
import com.joinhocus.horus.account.activity.NotificationType;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.FollowsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.entity.model.FollowEntityRequest;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.misc.follow.EntityType;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class FollowEntityHandler implements DefinedTypesWithUserHandler<FollowEntityRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends FollowEntityRequest> validator, Context context, Logger logger) throws Exception {
        FollowEntityRequest request = validator
                .check("id", req -> !Strings.isNullOrEmpty(req.getId()), "id cannot be empty")
                .check("type", req -> req.getType() != null, "type cannot be empty")
                .get();

        ObjectId mongoId = MongoIds.parseId(request.getId());
        if (mongoId == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("malformed entity id"));
        }

        return startPipeline(account, mongoId, request.getType());
    }

    private CompletableFuture<Response> startPipeline(UserAccount requester, ObjectId mongoId, EntityType type) {
        return MongoDatabase.getInstance().getRepo(FollowsRepo.class).follows(requester, mongoId).thenCompose(follows -> {
            if (follows) {
                return doUnfollow(requester, mongoId);
            }

            return doFollow(requester, mongoId, type);
        });
    }

    private CompletableFuture<Response> doFollow(UserAccount account, ObjectId id, EntityType type) {
        return MongoDatabase.getInstance().getRepo(FollowsRepo.class).follow(account, id, type).thenCompose(ignored -> {
            ActivityNotification notification = new ActivityNotification(
                    new ObjectId(),
                    account.getId(),
                    type,
                    Collections.singletonList(id),
                    new Date(),
                    NotificationType.FOLLOW,
                    new Document()
                        .append("followed", account.getId().toHexString())
                        .append("handle", account.getUsernames().getDisplay())
            );

            return MongoDatabase.getInstance().getRepo(ActivityRepo.class).insertNotification(notification).thenApply(aVoid -> {
                return Response.of(Response.Type.OKAY).append("followed", true);
            });
        });
    }

    private CompletableFuture<Response> doUnfollow(UserAccount account, ObjectId id) {
        return MongoDatabase.getInstance().getRepo(FollowsRepo.class).unfollow(account, id).thenApply(ignored -> {
            return Response.of(Response.Type.OKAY).append("followed", false);
        });
    }

    @Override
    public Class<? extends FollowEntityRequest> requestClass() {
        return FollowEntityRequest.class;
    }
}
