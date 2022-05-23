package com.joinhocus.horus.db.repos;

import com.google.common.collect.ImmutableList;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class InvitesRepo extends AsyncMongoRepo {
    public InvitesRepo() {
        super("hocus", "invites");
    }

    public CompletableFuture<Document> insertInvite(Invite invite) {
        return this.insertOne(invite.toDocument());
    }

    public CompletableFuture<Document> getInviteById(ObjectId id) {
        return this.findFirst(
                Filters.eq("_id", id),
                Function.identity()
        );
    }

    public CompletableFuture<Invite> getInviteByCode(String code) {
        return this.findFirst(
                Filters.eq("code", code),
                Invite::new
        );
    }

    public CompletableFuture<Boolean> checkExists(String code) {
        return this.checkExists(
                Filters.and(
                        Filters.eq("code", code),
                        Filters.exists("claimed", false)
                )
        );
    }

    public CompletableFuture<Boolean> checkTwitterExists(String code) {
        return this.checkExists(Filters.eq("twitterId", code));
    }

    public CompletableFuture<Boolean> checkEmailExists(String code) {
        return this.checkExists(Filters.eq("email", code));
    }

    public CompletableFuture<Void> claim(String code) {
        return this.updateOne(
                Filters.eq("code", code),
                Updates.set("claimed", true)
        ).thenApply(aBoolean -> null);
    }

    public CompletableFuture<ImmutableList<Invite>> getInvitesFrom(ObjectId inviter) {
        return this.find(Filters.eq("inviter", inviter), null, Invite::new);
    }
}
