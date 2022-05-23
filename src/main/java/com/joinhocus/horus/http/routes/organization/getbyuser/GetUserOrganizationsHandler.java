package com.joinhocus.horus.http.routes.organization.getbyuser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.organization.Organization;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GetUserOrganizationsHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class)
                .getOrganizations(account.getId())
                .thenApply(organizations -> {
                    JsonArray orgs = new JsonArray();
                    for (Organization organization : organizations) {
                        JsonObject object = new JsonObject();
                        object.addProperty("id", organization.getId().toHexString());
                        object.addProperty("name", organization.getName());
                        object.addProperty("handle", organization.getHandle());

                        JsonObject settings = new JsonObject();
                        settings.addProperty("logo", organization.getSettings().getLogo());
                        object.add("settings", settings);

                        orgs.add(object);
                    }

                    return Response.of(Response.Type.OKAY).append("organizations", orgs);
                });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
