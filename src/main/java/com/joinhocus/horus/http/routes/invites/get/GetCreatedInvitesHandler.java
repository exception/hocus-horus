package com.joinhocus.horus.http.routes.invites.get;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.twitter.TwitterAPI;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GetCreatedInvitesHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).getInvitesFrom(account.getId()).thenCompose(in -> {
            List<CompletableFuture<JsonObject>> futureInvites = new ArrayList<>();
            for (Invite invite : in) {
                if (!Strings.isNullOrEmpty(invite.getTwitterId())) {
                    futureInvites.add(TwitterAPI.fetchUserInfo(invite.getTwitterId()).thenApply(user -> {
                        JsonObject out = new JsonObject();
                        out.addProperty("display", "@" + user.getHandle());
                        out.addProperty("claimed", invite.isWasClaimed());

                        return out;
                    }));
                } else {
                    futureInvites.add(CompletableFuture.completedFuture(emailInviteToJson(invite)));
                }
            }

            return CompletableFutures.asList(futureInvites);
        }).thenApply(in -> {
            JsonArray invites = new JsonArray();
            for (JsonObject invite : in) {
                invites.add(invite);
            }

            return Response.of(Response.Type.OKAY).append("invites", invites);
        });
    }

    private JsonObject emailInviteToJson(Invite invite) {
        JsonObject object = new JsonObject();
        object.addProperty("display", invite.getEmail());
        object.addProperty("claimed", invite.isWasClaimed());

        return object;
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
