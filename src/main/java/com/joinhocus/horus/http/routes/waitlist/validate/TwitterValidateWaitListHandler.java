package com.joinhocus.horus.http.routes.waitlist.validate;

import com.google.common.base.Strings;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.db.repos.WaitListRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.waitlist.WaitlistUtil;
import com.joinhocus.horus.http.routes.waitlist.validate.model.TwitterValidateWaitListRequest;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.twitter.TwitterAPI;
import com.joinhocus.horus.twitter.oauth.OAuthAccountResponse;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class TwitterValidateWaitListHandler implements DefinedTypesHandler<TwitterValidateWaitListRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends TwitterValidateWaitListRequest> validator, Context context, Logger logger) throws Exception {
        TwitterValidateWaitListRequest request = validator
                .check("token", req -> !Strings.isNullOrEmpty(req.getToken()), "token cannot be empty")
                .check("verify", req -> !Strings.isNullOrEmpty(req.getVerify()), "verify cannot be empty")
                .check("twitterId", req -> !Strings.isNullOrEmpty(req.getTwitterId()), "twitterId cannot be empty")
                .get();

        return MongoDatabase.getInstance().getRepo(WaitListRepo.class).checkIfExists(request.getTwitterId()).thenCompose(document -> {
            if (document == null) {
                return handleNotInWaitlist(request.getTwitterId());
            }

            String id = document.getString("userId");
            return TwitterAPI.OAUTH.getOauthToken(request.getToken(), request.getVerify()).thenCompose(response -> {
                if (!response.getUserId().equals(id)) {
                    return handleJoinWaitlist(response);
                }

                return handleValidate(document);
            });
        });
    }

    private CompletableFuture<Response> handleNotInWaitlist(String userId) {
        return TwitterAPI.fetchUserInfo(userId).thenCompose(user -> {
            return MongoDatabase.getInstance().getRepo(WaitListRepo.class).createWaitlistUser(userId, user.getName());
        }).thenApply(res -> {
            WaitlistUtil.sendToSlack(userId, res.getValue());
            return Response.of(Response.Type.OKAY_CREATED).append("joined", true);
        });
    }

    private CompletableFuture<Response> handleJoinWaitlist(OAuthAccountResponse response) {
        return MongoDatabase.getInstance().getRepo(WaitListRepo.class).checkIfExists(response.getUserId()).thenCompose(document -> {
            if (document != null) {
                return wrap(Response.of(Response.Type.OKAY).append("twitter", response.getName()).append("existed", true));
            }

            return MongoDatabase.getInstance().getRepo(WaitListRepo.class)
                    .createWaitlistUser(response.getUserId(), response.getName()).thenApply(res -> {
                        return Response.of(Response.Type.OKAY_CREATED).append("joined", true);
                    });
        });
    }

    private CompletableFuture<Response> handleValidate(Document document) {
        boolean accepted = document.getBoolean("accepted", false);
        if (!accepted) {
            return wrap(Response.of(Response.Type.OKAY).append("accepted", false));
        }

        String inviteId = document.getString("inviteId");
        return runInvitePipeline(inviteId);
    }

    private CompletableFuture<Response> runInvitePipeline(String inviteId) {
        ObjectId id = MongoIds.parseId(inviteId);
        if (id == null) {
            return wrap(Response.of(Response.Type.OKAY).append("accepted", false));
        }
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class)
                .getInviteById(id)
                .thenCompose(document -> {
                    if (document == null) {
                        return wrap(Response.of(Response.Type.OKAY).append("accepted", false));
                    }

                    return wrap(Response.of(Response.Type.OKAY).append("accepted", true).append("code", document.getString("code")));
                });
    }

    @Override
    public Class<? extends TwitterValidateWaitListRequest> requestClass() {
        return TwitterValidateWaitListRequest.class;
    }
}
