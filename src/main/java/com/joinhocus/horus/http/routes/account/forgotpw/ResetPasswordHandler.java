package com.joinhocus.horus.http.routes.account.forgotpw;

import com.google.common.base.Strings;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.PasswordResetRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.forgotpw.model.ForgotPasswordTokenRequest;
import com.joinhocus.horus.misc.CompletableFutures;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ResetPasswordHandler implements DefinedTypesHandler<ForgotPasswordTokenRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends ForgotPasswordTokenRequest> validator, Context context, Logger logger) throws Exception {
        ForgotPasswordTokenRequest request = validator
                .check("token", req -> !Strings.isNullOrEmpty(req.getToken()), "token cannot be empty")
                .check("token", req -> {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        UUID.fromString(req.getToken());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }, "invalid token")
                .check("password", req -> !Strings.isNullOrEmpty(req.getPassword()), "password cannot be empty")
                .get();

        return MongoDatabase.getInstance().getRepo(PasswordResetRepo.class).getUserAccountFor(request.getToken())
                .thenCompose(account -> {
                    if (account == null) {
                        return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Unknown code or account"));
                    }
                    return runPippeline(request, account.getId());
                });
    }

    private CompletableFuture<Response> runPippeline(ForgotPasswordTokenRequest request, ObjectId accountId) {
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).changePassword(
                accountId,
                request.getPassword()
        ).thenCompose(success -> {
            if (!success) {
                return CompletableFutures.failedFuture(new RuntimeException("failed to update password, database did not acknowledge"));
            }

            return consumeCode(request.getToken());
        });
    }

    private CompletableFuture<Response> consumeCode(String token) {
        return MongoDatabase.getInstance().getRepo(PasswordResetRepo.class)
                .consumeCode(token)
                .thenCompose(aVoid -> {
                    // todo send email
                    return wrap(Response.of(Response.Type.OKAY));
                });
    }

    @Override
    public Class<? extends ForgotPasswordTokenRequest> requestClass() {
        return ForgotPasswordTokenRequest.class;
    }
}
