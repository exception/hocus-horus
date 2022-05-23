package com.joinhocus.horus.http.routes.invites.validate;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.twitter.TwitterAPI;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ValidateInviteHandler implements DefinedTypesHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String inviteId = context.pathParam("inviteId");
        if (Strings.isNullOrEmpty(inviteId)) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("invalid inviteId provided, cannot be empty"));
        }

        ObjectId actualId = MongoIds.parseId(inviteId);
        if (actualId == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Invalid inviteId provided"));
        }
        return runPipeline(actualId);
    }

    private CompletableFuture<Response> runPipeline(ObjectId inviteId) {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).getInviteById(inviteId).thenCompose(document -> {
            if (document == null) {
                // if not present we display it as claimed
                return wrap(Response.of(Response.Type.OKAY).append("claimed", true));
            }

            boolean claimed = document.getBoolean("claimed", false);
            if (claimed) {
                return wrap(Response.of(Response.Type.OKAY).append("claimed", true));
            }

            Invite invite = new Invite(document);
            if (invite.getTwitterId() != null) {
                return runTwitterCheck(invite.getTwitterId());
            }

            // we don't want to expose the invitee's email, so we won't return it.
            return wrap(Response.of(Response.Type.OKAY).append("claimed", false).append("isEmail", true));
        });
    }

    private CompletableFuture<Response> runTwitterCheck(String twitterId) {
        return TwitterAPI.fetchUserInfo(twitterId).thenApply(user -> {
            return Response.of(Response.Type.OKAY).append("claimed", false).append("twitterId", twitterId).append("handle", user.getHandle());
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
