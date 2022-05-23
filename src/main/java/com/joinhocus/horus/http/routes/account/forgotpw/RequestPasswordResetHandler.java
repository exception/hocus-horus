package com.joinhocus.horus.http.routes.account.forgotpw;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.AccountAuth;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.UserNames;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.PasswordResetRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.forgotpw.model.ForgotPasswordResetRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RequestPasswordResetHandler implements DefinedTypesHandler<ForgotPasswordResetRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends ForgotPasswordResetRequest> validator, Context context, Logger logger) throws Exception {
        ForgotPasswordResetRequest request = validator
                .check("login", req -> !Strings.isNullOrEmpty(req.getLogin()), "login cannot be empty")
                .check("login", req -> {
                    if (UserNames.NAME_PATTERN.matcher(req.getLogin()).find()) {
                        return true;
                    }

                    return AccountAuth.isValidEmail(req.getLogin());
                }, "login field must be an email or username")
                .get();
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).findBasicByLogin(request.getLogin()).thenCompose(account -> {
            if (account == null) {
                return wrap(Response.of(Response.Type.OKAY));
            }

            UUID uuid = UUID.randomUUID();
            return createResetCode(account, uuid, context.ip());
        });
    }

    private CompletableFuture<Response> createResetCode(UserAccount account, UUID uuid, String ipAddress) {
        return MongoDatabase.getInstance().getRepo(PasswordResetRepo.class)
                .createResetCode(account, uuid, ipAddress)
                .thenCompose(opt -> {
                    return sendEmail();
                }).thenApply(aVoid -> {
                    return Response.of(Response.Type.OKAY);
                });
    }

    private CompletableFuture<Void> sendEmail() {
        // todo
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<? extends ForgotPasswordResetRequest> requestClass() {
        return ForgotPasswordResetRequest.class;
    }
}
