package com.joinhocus.horus.http.routes.account.create;

import com.google.common.base.Strings;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.WaitListRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.create.model.VerifyComboRequest;
import com.joinhocus.horus.twitter.TwitterAPI;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ValidateInviteTwitterComboHandler implements DefinedTypesHandler<VerifyComboRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends VerifyComboRequest> validator, Context context, Logger logger) throws Exception {
        VerifyComboRequest request = validator
                .check("inviteCode", req -> !Strings.isNullOrEmpty(req.getInviteCode()), "inviteCode cannot be empty")
                .check("twitterId", req -> !Strings.isNullOrEmpty(req.getTwitterId()), "twitterId cannot be empty")
                .get();

        return MongoDatabase.getInstance().getRepo(WaitListRepo.class)
                .checkCombination(request.getInviteCode(), request.getTwitterId())
                .thenCompose(valid -> {
                    if (!valid) {
                        return wrap(Response.of(Response.Type.OKAY).append("valid", false));
                    }

                    return doComplexPipeline(request.getTwitterId());
                });
    }

    private CompletableFuture<Response> doComplexPipeline(String twitterId) {
        return TwitterAPI.fetchUserInfo(twitterId).thenApply(user -> {
            return Response.of(Response.Type.OKAY).append("valid", true).append("handle", user.getHandle());
        });
    }

    @Override
    public Class<? extends VerifyComboRequest> requestClass() {
        return VerifyComboRequest.class;
    }
}
