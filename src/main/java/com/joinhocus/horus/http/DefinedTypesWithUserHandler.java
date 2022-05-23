package com.joinhocus.horus.http;

import com.joinhocus.horus.account.AccountAuth;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.misc.CompletableFutures;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public interface DefinedTypesWithUserHandler<Request> extends DefinedTypesHandler<Request> {

    @Override
    default CompletableFuture<Response> handle(BodyValidator<? extends Request> validator, Context context, Logger logger) throws Exception {
        String token = context.header("Authorization");
        if (token == null) {
            throw new BadRequestResponse("Not logged in");
        }
        if (!token.startsWith("Bearer ")) {
            throw new BadRequestResponse("Malformed Authorization header");
        }
        token = token.replaceFirst("Bearer ", "");
        String account = AccountAuth.getAccountFromJwt(token);
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class)
                .findByLogin(account)
                .thenCompose(userAccount -> {
                    if (userAccount == null) {
                        return wrap(Response.of(Response.Type.UNAUTHORIZED));
                    }

                    try {
                        return handle(userAccount, validator, context, logger);
                    } catch (Exception e) {
                        return CompletableFutures.failedFuture(e);
                    }
                });
    }

    CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends Request> validator, Context context, Logger logger) throws Exception;
}
