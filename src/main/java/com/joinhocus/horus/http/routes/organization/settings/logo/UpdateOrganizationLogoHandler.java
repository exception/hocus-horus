package com.joinhocus.horus.http.routes.organization.settings.logo;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.MongoIds;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class UpdateOrganizationLogoHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String id = context.queryParam("orgId");
        if (Strings.isNullOrEmpty(id)) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("orgId cannot be an empty query param"));
        }
        ObjectId orgId = MongoIds.parseId(id);
        if (orgId == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Invalid organization id provided"));
        }
        UploadedFile file = context.uploadedFile("file");
        if (file == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("file cannot be empty"));
        }

        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).isMemberOf(orgId, account.getId()).thenCompose(isMember -> {
            if (!isMember) {
                return wrap(Response.of(Response.Type.UNAUTHORIZED));
            }

            return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).setOrganizationLogo(
                    orgId,
                    file
            ).thenApply(success -> {
                if (!success) {
                    return Response.of(Response.Type.BAD_REQUEST).setMessage("failed to update organization logo");
                }

                return Response.of(Response.Type.OKAY);
            });
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
