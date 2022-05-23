package com.joinhocus.horus.http.routes.account.changepw;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.changepw.model.ChangePasswordRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ChangePasswordHandler implements DefinedTypesWithUserHandler<ChangePasswordRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends ChangePasswordRequest> validator, Context context, Logger logger) throws Exception {
        ChangePasswordRequest request = validator
                .check("current", req -> !Strings.isNullOrEmpty(req.getCurrent()), "current cannot be empty")
                .check("changeTo", req -> !Strings.isNullOrEmpty(req.getChangeTo()), "changeTo cannot be empty")
                .get();

        boolean isValid = account.passwordsMatch(request.getCurrent());
        if (!isValid) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Incorrect current password provided."));
        }

        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).changePassword(account.getId(), request.getChangeTo()).thenApply(success -> {
            if (success) {
                return Response.of(Response.Type.OKAY);
            }

            return Response.of(Response.Type.BAD_REQUEST).setMessage("Failed to update password");
        });
    }

    @Override
    public Class<? extends ChangePasswordRequest> requestClass() {
        return ChangePasswordRequest.class;
    }
}
