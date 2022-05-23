package com.joinhocus.horus.http.routes.waitlist.verify;

import com.google.common.base.Strings;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.WaitListRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.waitlist.WaitlistUtil;
import com.joinhocus.horus.http.routes.waitlist.verify.model.VerifyTwitterOAuthRequest;
import com.joinhocus.horus.twitter.TwitterAPI;
import com.joinhocus.horus.twitter.oauth.OAuthAccountResponse;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class VerifyWaitlistTwitterHandler implements DefinedTypesHandler<VerifyTwitterOAuthRequest> {

    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends VerifyTwitterOAuthRequest> validator, Context context, Logger logger) throws Exception {
        VerifyTwitterOAuthRequest request = validator
                .check("token", req -> !Strings.isNullOrEmpty(req.getToken()), "token cannot be empty")
                .check("verify", req -> !Strings.isNullOrEmpty(req.getVerify()), "verify cannot be empty")
                .get();

        return TwitterAPI.OAUTH.getOauthToken(request.getToken(), request.getVerify()).thenCompose(this::handleExists);
    }

    private CompletableFuture<Response> handleExists(OAuthAccountResponse response) {
        return MongoDatabase.getInstance().getRepo(WaitListRepo.class).checkIfExists(response.getUserId()).thenCompose(res -> {
            if (res != null) {
                return wrap(Response.of(Response.Type.OKAY).append("twitter", response.getName()).append("existed", true));
            }
            return joinWaitlist(response);
        });
    }

    public CompletableFuture<Response> joinWaitlist(OAuthAccountResponse response) {
        return MongoDatabase.getInstance().getRepo(WaitListRepo.class)
                .createWaitlistUser(response.getUserId(), response.getName())
                .thenApply(res -> {
                    WaitlistUtil.sendToSlack(res.getKey().getUserId(), res.getValue()); // this gets executed in a seperate thread pool
                    return Response.of(Response.Type.OKAY).append("twitter", res.getKey().getName()).append("joined", true);
                });
    }

    @Override
    public Class<? extends VerifyTwitterOAuthRequest> requestClass() {
        return VerifyTwitterOAuthRequest.class;
    }
}
