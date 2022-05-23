package com.joinhocus.horus.http.routes.account.code;

import com.google.common.base.Strings;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.code.model.InviteCodeRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ValidateInviteCodeHandler implements DefinedTypesHandler<InviteCodeRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends InviteCodeRequest> validator, Context context, Logger logger) throws Exception {
        InviteCodeRequest request = validator
                .check("code", req -> !Strings.isNullOrEmpty(req.getCode()), "code cannot be empty")
                .get();
        return runPipeline(request.getCode());
    }

    private CompletableFuture<Response> runPipeline(String code) {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class)
                .getInviteByCode(code)
                .thenCompose(invite -> {
                    if (invite == null) {
                        return wrap(Response.of(Response.Type.OKAY).append("valid", false));
                    }
                    if (invite.isWasClaimed()) {
                        return wrap(Response.of(Response.Type.OKAY).append("valid", false));
                    }

                    return wrap(Response.of(Response.Type.OKAY).append("valid", true));
                });
    }

    @Override
    public Class<? extends InviteCodeRequest> requestClass() {
        return InviteCodeRequest.class;
    }
}
