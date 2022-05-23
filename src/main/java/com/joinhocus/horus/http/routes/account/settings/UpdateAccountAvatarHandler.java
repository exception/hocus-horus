package com.joinhocus.horus.http.routes.account.settings;

import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class UpdateAccountAvatarHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        UploadedFile file = context.uploadedFile("file");
        if (file == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("file cannot be empty"));
        }

        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).setAvatar(
                account.getId(),
                file
        ).thenApply(success -> {
            if (!success) {
                return Response.of(Response.Type.BAD_REQUEST).setMessage("failed to update avatar");
            }

            return Response.of(Response.Type.OKAY);
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
