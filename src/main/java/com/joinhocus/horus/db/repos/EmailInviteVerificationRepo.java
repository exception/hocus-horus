package com.joinhocus.horus.db.repos;

import com.google.common.base.Preconditions;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class EmailInviteVerificationRepo extends AsyncMongoRepo {
    public EmailInviteVerificationRepo() {
        super("hocus", "email_invite_verification");
    }

    public CompletableFuture<Void> insertVerification(Invite invite, String code) {
        Preconditions.checkNotNull(invite.getEmail());

        return this.insertOne(new Document()
                .append("inviteId", invite.getInviteId())
                .append("code", code)
        ).thenApply(opt -> null);
    }

    public CompletableFuture<Document> getByCode(String code) {
        return this.findFirst(Filters.eq("code", code), Function.identity());
    }
}
