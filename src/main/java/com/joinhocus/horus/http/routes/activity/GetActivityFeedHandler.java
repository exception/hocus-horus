package com.joinhocus.horus.http.routes.activity;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.activity.ActivityNotification;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.misc.PaginatedList;
import com.joinhocus.horus.organization.Organization;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GetActivityFeedHandler implements DefinedTypesWithUserHandler<EmptyRequest> {

    private final Gson GSON = new GsonBuilder().serializeNulls()
            .create();

    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        int page = getIntParam("page", context);
        return doGetFeed(account, page);
    }

    private CompletableFuture<Response> doGetFeed(UserAccount account, int page) {
        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).getOrganizations(account.getId()).thenApply(orgs -> {
            return ImmutableList.<ObjectId>builder()
                    .add(account.getId())
                    .addAll(orgs.stream().map(Organization::getId).collect(Collectors.toList()))
                    .build();
        }).thenCompose(ids -> {
            return MongoDatabase.getInstance().getRepo(ActivityRepo.class).getActivity(
                    account.getId(),
                    ids,
                    page,
                    20
            );
        }).thenCompose(notifications -> {
            return doActivity(notifications, account);
        });
    }

    private CompletableFuture<Response> doActivity(PaginatedList<ActivityNotification> notifications, UserAccount requester) {
        return MongoDatabase.getInstance().getRepo(ActivityRepo.class).markSeen(
                notifications.getData().stream().map(ActivityNotification::getId).collect(Collectors.toList()),
                requester.getId()
        ).thenCompose(ignored -> {
            List<CompletableFuture<JsonObject>> futures = new ArrayList<>(notifications.getData().size());
            for (ActivityNotification notification : notifications.getData()) {
                futures.add(notificationToJson(notification));
            }

            return CompletableFutures.asList(futures);
        }).thenApply(objects -> {
            JsonArray array = new JsonArray();
            for (JsonObject object : objects) {
                array.add(object);
            }

            return Response.of(Response.Type.OKAY).append("activity", array).append("pagination", notifications.getPaginationData());
        });
    }

    private CompletableFuture<JsonObject> notificationToJson(ActivityNotification notif) {
        JsonObject object = new JsonObject();
        object.addProperty("when", notif.getWhen().getTime());
        object.addProperty("type", notif.getNotificationType().name());
        object.addProperty("wasSeen", notif.isWasSeen());
        object.addProperty("entity", notif.getEntityType().name());

        if (notif.getExtra() != null) {
            JsonObject extra = GSON.fromJson(notif.getExtra().toJson(), JsonObject.class);
            object.add("extra", extra);
        }

        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).findById(notif.getActor()).thenApply(account -> {
            JsonObject actor = new JsonObject();
            actor.addProperty("id", account.getId().toHexString());
            actor.addProperty("name", account.getName());
            actor.addProperty("handle", account.getUsernames().getDisplay());
            actor.addProperty("avatar", account.getAvatar());
            object.add("actor", actor);

            return object;
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
