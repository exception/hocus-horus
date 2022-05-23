package com.joinhocus.horus.http.routes.account.setup;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.AccountType;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.setup.model.SetupAccountRequest;
import com.joinhocus.horus.misc.CompletableFutures;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class SetupAccountHandler implements DefinedTypesWithUserHandler<SetupAccountRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends SetupAccountRequest> validator, Context context, Logger logger) throws Exception {
        SetupAccountRequest request = validator
                .check("selected", req -> !Strings.isNullOrEmpty(req.getSelected()), "selected cannot be empty")
                .get();
        if (account.isFinishedAccountSetup()) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("This account has already been setup"));
        }
        AccountType type = AccountType.valueOf(request.getSelected().toUpperCase());
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).setAccountType(
                account.getId(),
                type
        ).thenCompose(success -> {
            if (!success) {
                return CompletableFutures.failedFuture(new IllegalStateException("database failed to acknowledge update"));
            }

            return wrap(Response.of(Response.Type.OKAY));
        });
    }

    @Override
    public Class<? extends SetupAccountRequest> requestClass() {
        return SetupAccountRequest.class;
    }
}
