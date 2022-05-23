package com.joinhocus.horus.db.repos;

import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.db.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class PasswordResetRepo extends AsyncMongoRepo {
    public PasswordResetRepo() {
        super("hocus", "password_resets");
    }

    public CompletableFuture<Document> createResetCode(UserAccount account, UUID code, String ip) {
        return this.insertOne(new Document()
                .append("for", account.getId())
                .append("code", code.toString())
                .append("createdAt", System.currentTimeMillis())
                .append("requestingIp", ip)
        );
    }

    public CompletableFuture<Void> consumeCode(String code) {
        return this.updateOne(Filters.eq("code", code), Updates.set("consumed", true)).thenApply(success -> null);
    }

    public CompletableFuture<UserAccount> getUserAccountFor(String token) {
        return this.findFirst(Filters.and(
                Filters.eq("code", token),
                Filters.exists("consumed", false)
        ), Function.identity())
                .thenCompose(opt -> {
                    if (opt == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    ObjectId forUser = opt.getObjectId("for");
                    return MongoDatabase.getInstance().getRepo(AccountsRepo.class)
                            .findById(forUser);
                });
    }
}
