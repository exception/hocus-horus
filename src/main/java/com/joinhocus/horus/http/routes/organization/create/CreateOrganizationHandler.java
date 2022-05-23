package com.joinhocus.horus.http.routes.organization.create;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.UserNames;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.organization.create.model.CreateOrganizationRequest;
import com.joinhocus.horus.misc.HandlesUtil;
import com.joinhocus.horus.organization.Organization;
import com.joinhocus.horus.organization.SimpleOrganization;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class CreateOrganizationHandler implements DefinedTypesWithUserHandler<CreateOrganizationRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends CreateOrganizationRequest> validator, Context context, Logger logger) throws Exception {
        CreateOrganizationRequest request = validator
                .check("name", req -> !Strings.isNullOrEmpty(req.getName()), "name cannot be empty")
                .check("name", req -> req.getName().trim().length() <= 50, "name cannot be longer than 50 characters")
                .check("handle", req -> !Strings.isNullOrEmpty(req.getHandle()), "username cannot be empty")
                .check("handle", req -> UserNames.NAME_PATTERN.matcher(req.getHandle()).find(), "handle too long or contains invalid characters")
                .get();

        return runPipeline(request, account);
    }

    private CompletableFuture<Response> runPipeline(CreateOrganizationRequest request, UserAccount account) {
        return HandlesUtil.isHandleAvailable(request.getHandle()).thenCompose(available -> {
            if (!available) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("That handle is not available"));
            }

            return createOrganization(request, account);
        });
    }

    private CompletableFuture<Response> createOrganization(CreateOrganizationRequest request, UserAccount account) {
        Organization organization = new SimpleOrganization(
                request.getName(),
                request.getHandle().toLowerCase()
        );

        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).createOrganization(
                organization,
                account
        ).thenApply(document -> {
            if (document != null) {
                return Response.of(Response.Type.OKAY_CREATED).append("organizationId", document.getObjectId("_id").toHexString());
            }

            return Response.of(Response.Type.BAD_REQUEST).setMessage("failed to create organization");
        });
    }

    @Override
    public Class<? extends CreateOrganizationRequest> requestClass() {
        return CreateOrganizationRequest.class;
    }
}
