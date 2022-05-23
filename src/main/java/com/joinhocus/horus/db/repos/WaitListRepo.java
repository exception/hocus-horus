package com.joinhocus.horus.db.repos;

import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.misc.Pair;
import com.joinhocus.horus.misc.strgen.WordStringGenerator;
import com.joinhocus.horus.twitter.oauth.OAuthAccountResponse;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WaitListRepo extends AsyncMongoRepo {

    private final WordStringGenerator GENERATOR = new WordStringGenerator();

    public WaitListRepo() {
        super("hocus", "waitlist");
    }

    public CompletableFuture<Boolean> delete(Document document) {
        return this.deleteOne(document);
    }

    public CompletableFuture<Document> checkIfExists(String userId) {
        return this.findFirst(Filters.or(
                Filters.eq("userId", userId),
                Filters.eq("name", userId)
        ), Function.identity());
    }

    public CompletableFuture<Pair<OAuthAccountResponse, String>> createWaitlistUser(String userId, String name) {
        String code = GENERATOR.generate(4);
        Invite invite = new Invite(
                userId,
                null,
                null,
                null,
                code
        );
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).insertInvite(invite).thenCompose(res -> {
            if (res == null) {
                return CompletableFutures.failedFuture(new RuntimeException("failed to insert invite"));
            }

            return this.insertOne(new Document()
                    .append("userId", userId)
                    .append("name", name)
                    .append("waitingSince", System.currentTimeMillis())
                    .append("accepted", false)
                    .append("inviteId", res.getObjectId("_id").toHexString())
            ).thenApply(doc -> new Pair<>(new OAuthAccountResponse(userId, name), res.getObjectId("_id").toHexString()));
        });
    }

    public CompletableFuture<Boolean> checkCombination(String inviteCode, String twitterId) {
        return this.findFirst(Filters.and(
                Filters.eq("inviteCode", inviteCode),
                Filters.eq("userId", twitterId)
        ), Objects::nonNull);
    }

    public CompletableFuture<Void> consume(String invite) {
        return this.deleteOne(Filters.eq(
                "inviteCode", invite
        )).thenApply(aBoolean -> null);
    }

    public CompletableFuture<Document> getByInviteId(String inviteId) {
        return this.findFirst(Filters.eq("inviteId", inviteId), Function.identity());
    }
}
