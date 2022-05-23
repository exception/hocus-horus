package com.joinhocus.horus.http.routes.invites.token;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.EmailInviteVerificationRepo;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.invites.InviteRequest;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.misc.Environment;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.misc.email.EmailClient;
import com.wildbit.java.postmark.client.data.model.message.Message;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SendVerificationEmailHandler implements DefinedTypesHandler<InviteRequest> {

    private final String BASE_URL = Environment.current().getUrl() + "/flow/email";

    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends InviteRequest> validator, Context context, Logger logger) throws Exception {
        decorateWithRateLimit(context, 1, TimeUnit.MINUTES);
        InviteRequest request = validator
                .check("inviteId", req -> !Strings.isNullOrEmpty(req.getInviteId()), "inviteId cannot be empty")
                .get();
        ObjectId inviteId = MongoIds.parseId(request.getInviteId());
        if (inviteId == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Invalid inviteId provided"));
        }
        return runPipeline(inviteId);
    }

    private CompletableFuture<Response> runPipeline(ObjectId inviteId) {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).getInviteById(inviteId).thenCompose(document -> {
            if (document == null) {
                return wrap(Response.of(Response.Type.OKAY)); // fake
            }
            boolean claimed = document.getBoolean("claimed", false);
            if (claimed) {
                return wrap(Response.of(Response.Type.OKAY).append("claimed", true));
            }

            Invite invite = new Invite(document);
            return generateCodeAndEmail(invite);
        });
    }

    private CompletableFuture<Response> generateCodeAndEmail(Invite invite) {
        String code = UUID.randomUUID().toString();
        return MongoDatabase.getInstance().getRepo(EmailInviteVerificationRepo.class)
                .insertVerification(invite, code)
                .thenCompose(aVoid -> {
                    try {
                        Message message = new Message(invite.getEmail(), invite.getEmail(), "Invite Code", BASE_URL + code);
                        EmailClient.getInstance().getClient().deliverMessage(message);
                        return wrap(Response.of(Response.Type.OKAY).append("sent", true));
                    } catch (Exception e) {
                        return CompletableFutures.failedFuture(e);
                    }
                });
    }

    @Override
    public Class<? extends InviteRequest> requestClass() {
        return InviteRequest.class;
    }
}
