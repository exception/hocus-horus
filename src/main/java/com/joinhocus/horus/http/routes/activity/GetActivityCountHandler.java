package com.joinhocus.horus.http.routes.activity;

import com.google.common.collect.ImmutableList;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.organization.Organization;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GetActivityCountHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).getOrganizations(account.getId()).thenApply(orgs -> {
            return ImmutableList.<ObjectId>builder()
                    .add(account.getId())
                    .addAll(orgs.stream().map(Organization::getId).collect(Collectors.toList()))
                    .build();
        }).thenCompose(ids -> {
            return MongoDatabase.getInstance().getRepo(ActivityRepo.class).countActivity(
                    account.getId(),
                    ids
            );
        }).thenApply(notifications -> {
            return Response.of(Response.Type.OKAY).append("amount", notifications);
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
