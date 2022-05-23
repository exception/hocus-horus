package com.joinhocus.horus.db.repos;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.AccountAuth;
import com.joinhocus.horus.account.AccountStatus;
import com.joinhocus.horus.account.AccountType;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.http.routes.account.create.model.CreateAccountRequest;
import com.joinhocus.horus.misc.Spaces;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import io.javalin.http.UploadedFile;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

public class AccountsRepo extends AsyncMongoRepo {
    public AccountsRepo() {
        super("hocus", "accounts");
    }

    @Override
    public void postBoot() {
        createIndexes(Lists.newArrayList(
                new IndexModel(Indexes.ascending("user.name"), INDEX_BACKGROUND),
                new IndexModel(Indexes.ascending("email"), INDEX_BACKGROUND)
        ));
    }

    public CompletableFuture<AccountStatus> checkAccount(CreateAccountRequest request) {
        // if it's null, it means nothing matched and it's available
        return this.findFirst(Filters.or(
                Filters.eq("user.name", request.getUsername().toLowerCase()),
                Filters.eq("email", request.getEmail().toLowerCase())
        ), doc -> {
            if (doc == null) return AccountStatus.AVAILABLE;
            if (request.getEmail().equalsIgnoreCase(doc.getString("email"))) return AccountStatus.EMAIL_TAKEN;
            String remoteName = doc.get("user", new Document()).getString("name");
            if (request.getUsername().equalsIgnoreCase(remoteName)) return AccountStatus.USERNAME_TAKEN;
            return AccountStatus.UNKNOWN;
        });
    }

    public CompletableFuture<UserAccount> createAccount(CreateAccountRequest request) {
        Document created = UserAccount.generateAccount(request);
        return this.insertOne(created).thenApply(UserAccount::new);
    }

    public CompletableFuture<UserAccount> findById(ObjectId id) {
        return this.findFirst(
                Filters.eq("_id", id),
                UserAccount::new
        );
    }

    public CompletableFuture<UserAccount> findByHandle(String handle) {
        return this.findFirst(
                Filters.eq("user.name", handle),
                UserAccount::new
        );
    }

    public CompletableFuture<UserAccount> findByLogin(String field) {
        return this.findFirst(
                Filters.or(
                        Filters.eq("user.name", field.toLowerCase()),
                        Filters.eq("email", field)
                ),
                UserAccount::new
        );
    }

    public CompletableFuture<UserAccount> findBasicByLogin(String field) {
        return this.findFirst(
                Filters.or(
                        Filters.eq("user.name", field.toLowerCase()),
                        Filters.eq("email", field)
                ),
                Projections.include("name", "email", "password", "user"),
                UserAccount::new
        );
    }

    public CompletableFuture<Boolean> isUsernameAvailable(String username) {
        return this.checkExists(Filters.eq("user.name", username.toLowerCase()));
    }

    public CompletableFuture<ImmutableList<JsonObject>> searchByHandle(String username) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(username), Pattern.CASE_INSENSITIVE);
        return this.find(Filters.regex("user.name", pattern), Projections.fields(
                Projections.excludeId(),
                Projections.include("user.name"),
                Projections.include("name"),
                Projections.include("avatar")
        ), doc -> {
            JsonObject object = new JsonObject();
            Document user = doc.get("user", Document.class);
            object.addProperty("handle", user.getString("name"));
            object.addProperty("name", doc.getString("name"));
            object.addProperty("avatar", doc.getString("avatar"));
            return object;
        });
    }

    public CompletableFuture<ImmutableList<ObjectId>> getAllByHandles(List<String> handles) {
        return this.find(Filters.in("user.name", handles), null, doc -> doc.getObjectId("_id"));
    }

    public CompletableFuture<Boolean> setAccountType(ObjectId id, AccountType type) {
        return this.updateOne(Filters.eq("_id", id), Updates.set("type", type.name()));
    }

    public CompletableFuture<Boolean> changePassword(ObjectId id, String newPassword) {
        char[] password = newPassword.toCharArray();
        try {
            String hash = AccountAuth.ARGON_2.hash(
                    22,
                    65536,
                    1,
                    password
            );

            return this.updateOne(
                    Filters.eq("_id", id),
                    Updates.set("password", hash)
            );
        } finally {
            AccountAuth.ARGON_2.wipeArray(password);
        }
    }

    public CompletableFuture<Boolean> setAvatar(ObjectId accountId, UploadedFile file) {
        CompletableFuture<String> put = new CompletableFuture<>();
        ForkJoinPool.commonPool().submit(() -> {
            try {
                String url = Spaces.uploadImage(file, "avatars", accountId.toHexString());
                put.complete(url);
            } catch (Exception e) {
                put.completeExceptionally(e);
            }
        });

        return put.thenCompose(url -> {
            return this.updateOne(Filters.eq("_id", accountId), Updates.set("avatar", url));
        });
    }

    public CompletableFuture<Void> consumeInvite(ObjectId accountId) {
        return this.updateOne(
                Filters.eq("_id", accountId),
                Updates.inc("extra.invites", -1)
        ).thenApply(ignored -> null);
    }
}
