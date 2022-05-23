package com.joinhocus.horus.http.routes.invites.create;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.AccountAuth;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.invites.create.model.CreateInviteRequest;
import com.joinhocus.horus.misc.strgen.WordStringGenerator;
import com.joinhocus.horus.twitter.TwitterAPI;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class CreateInviteHandler implements DefinedTypesWithUserHandler<CreateInviteRequest> {

    private final WordStringGenerator stringGenerator = new WordStringGenerator();

    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends CreateInviteRequest> validator, Context context, Logger logger) throws Exception {
        CreateInviteRequest request = validator
                .check("sendTo", req -> !Strings.isNullOrEmpty(req.getSendTo()), "sendTo cannot be empty")
                .get();

        int invitesLeft = account.getExtra().getInteger("invites", 0);
        if (invitesLeft <= 0) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("You don't have any more invites left!"));
        }

        String sendTo = request.getSendTo();
        boolean isEmail = AccountAuth.isValidEmail(sendTo);
        String code = stringGenerator.generate(4);

        if (!isEmail) {
            return TwitterAPI.searchUser(sendTo).thenCompose(user -> {
                if (user == null) {
                    return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Couldn't find a Twitter user with that handle"));
                }

                Invite invite = new Invite(user.getId(), null, account.getId(), request.getOrgId(), code);
                return checkTwitterExists(user.getId(), account, invite);
            }).exceptionally(err -> {
                return Response.of(Response.Type.BAD_REQUEST).setMessage("Couldn't find a Twitter user with that handle");
            });
        }

        Invite invite = new Invite(null, sendTo, account.getId(), request.getOrgId(), code);
        return checkEmailExists(sendTo, account, invite);
    }

    private CompletableFuture<Response> checkTwitterExists(String twitter, UserAccount account, Invite invite) {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).checkTwitterExists(twitter).thenCompose(exists -> {
            if (exists) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("That user already has an invite to Hocus!"));
            }

            return insertInvite(account, invite);
        });
    }

    private CompletableFuture<Response> checkEmailExists(String email, UserAccount account, Invite invite) {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).checkEmailExists(email).thenCompose(exists -> {
            if (exists) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("That user already has an invite to Hocus!"));
            }

            return insertInvite(account, invite);
        });
    }

    private CompletableFuture<Response> insertInvite(UserAccount account, Invite invite) {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).insertInvite(invite).thenCompose(document -> {
            if (document == null) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Failed to create invite"));
            }

            return consumeInvite(account, invite);
        });
    }

    private CompletableFuture<Response> consumeInvite(UserAccount account, Invite invite) {
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).consumeInvite(account.getId()).thenApply(ignored -> {
            // TODO: send email/twitter, slack alert
            return Response.of(Response.Type.OKAY)
                    .append("code", invite.getCode())
                    .append("inviteId", invite.getInviteId());
        });
    }

    @Override
    public Class<? extends CreateInviteRequest> requestClass() {
        return CreateInviteRequest.class;
    }
}
