package com.joinhocus.horus.http.routes.account.login;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.AccountAuth;
import com.joinhocus.horus.account.UserNames;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.login.model.AccountLoginRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class AccountLoginHandler implements DefinedTypesHandler<AccountLoginRequest> {

    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends AccountLoginRequest> validator, Context context, Logger logger) throws Exception {
        AccountLoginRequest request = validator
                .check("login", req -> !Strings.isNullOrEmpty(req.getLogin()), "login cannot be empty")
                .check("login", req -> {
                    if (UserNames.NAME_PATTERN.matcher(req.getLogin()).find()) {
                        return true;
                    }

                    return AccountAuth.isValidEmail(req.getLogin());
                }, "login field must be an email or username")
                .check("password", req -> !Strings.isNullOrEmpty(req.getPassword()), "password cannot be empty")
                .get();
        AccountsRepo repo = MongoDatabase.getInstance().getRepo(AccountsRepo.class);
        return repo.findBasicByLogin(
                request.getLogin()
        ).thenApply(account -> {
            if (account == null) {
                return Response.of(Response.Type.BAD_REQUEST).setMessage("Invalid login or password provided");
            }

            if (!account.passwordsMatch(request.getPassword())) {
                return Response.of(Response.Type.FORBIDDEN).setMessage("Invalid login or password provided");
            }

            return Response.of(Response.Type.OKAY).append("token", AccountAuth.getToken(account));
        });
    }

    @Override
    public Class<? extends AccountLoginRequest> requestClass() {
        return AccountLoginRequest.class;
    }
}
