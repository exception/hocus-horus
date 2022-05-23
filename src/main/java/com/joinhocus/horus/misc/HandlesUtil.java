package com.joinhocus.horus.misc;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@UtilityClass
public class HandlesUtil {

    public final Pattern TAG_PATTERN = Pattern.compile("@\\[(.*?)]");

    public CompletableFuture<Boolean> isHandleAvailable(String handle) {
        return CompletableFutures.combine(
                MongoDatabase.getInstance().getRepo(AccountsRepo.class).isUsernameAvailable(handle),
                MongoDatabase.getInstance().getRepo(OrganizationRepo.class).isHandleAvailable(handle),
                (resA, resB) -> !resA && !resB
        ).toCompletableFuture();
    }

    public CompletableFuture<ImmutableList<JsonObject>> getHandlesByQuery(String query) {
        return CompletableFutures.combine(
                MongoDatabase.getInstance().getRepo(AccountsRepo.class).searchByHandle(query),
                MongoDatabase.getInstance().getRepo(OrganizationRepo.class).searchByHandle(query),
                (resA, resB) -> ImmutableList.<JsonObject>builder()
                        .addAll(resA)
                        .addAll(resB)
                        .build()
        ).toCompletableFuture();
    }

    public CompletableFuture<List<ObjectId>> getEntitiesBy(List<String> handles) {
        return CompletableFutures.combine(
                MongoDatabase.getInstance().getRepo(AccountsRepo.class).getAllByHandles(handles),
                MongoDatabase.getInstance().getRepo(OrganizationRepo.class).getAllByHandles(handles),
                (resA, resB) -> {
                    List<ObjectId> ids = new ArrayList<>(resA.size() + resB.size());
                    ids.addAll(resA);
                    ids.addAll(resB);
                    return ids;
                }
        ).toCompletableFuture();
    }

}
