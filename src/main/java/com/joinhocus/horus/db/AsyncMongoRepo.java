package com.joinhocus.horus.db;

import com.google.common.collect.ImmutableList;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class AsyncMongoRepo {

    private final String databaseName, collection;
    private MongoDatabase database;
    private MongoCollection<Document> mongoCollection;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final IndexOptions INDEX_BACKGROUND = new IndexOptions().background(true);

    public AsyncMongoRepo(String databaseName, String collection) {
        this.databaseName = databaseName;
        this.collection = collection;
    }

    public void setDatabase(MongoDatabase database) {
        this.database = database;
        this.mongoCollection = database.getClient()
                .getDatabase(databaseName)
                .getCollection(collection);
    }

    public void postBoot() {
    }

    protected void createIndexes(List<IndexModel> indexes) {
        this.getCollection().createIndexes(indexes, (strings, throwable) -> {
            if (throwable != null) {
                logger.error("", throwable);
                return;
            }

            logger.info("Index create {}", strings);
        });
    }

    protected MongoCollection<Document> getCollection(String name) {
        return this.database.getClient().getDatabase(databaseName).getCollection(name);
    }

    protected MongoCollection<Document> getCollection() {
        return this.mongoCollection;
    }

    protected CompletableFuture<Boolean> updateOne(Bson filter, Bson update) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        this.getCollection()
                .updateOne(filter, update, (updateResult, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(updateResult.wasAcknowledged());
                });
        return out;
    }

    protected CompletableFuture<Boolean> updateMany(Bson filter, Bson update) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        this.getCollection()
                .updateMany(filter, update, (updateResult, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(updateResult.wasAcknowledged());
                });
        return out;
    }

    protected <O> CompletableFuture<ImmutableList<O>> findPaginated(Bson filter, Bson sort, int page, int limit, @NotNull Function<Document, O> mapper) {
        CompletableFuture<ImmutableList<O>> out = new CompletableFuture<>();
        List<O> partial = new ArrayList<>();
        //noinspection NullableProblems
        this.getCollection()
                .find(filter)
                .sort(sort)
                .skip(limit * page)
                .limit(limit)
                .map(mapper::apply)
                .into(partial, (os, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(ImmutableList.copyOf(os));
                });
        return out;
    }

    protected <O> CompletableFuture<ImmutableList<O>> find(Bson filter, Bson projection, @NotNull Function<Document, O> mapper) {
        CompletableFuture<ImmutableList<O>> out = new CompletableFuture<>();
        List<O> partial = new ArrayList<>();
        //noinspection NullableProblems
        this.getCollection()
                .find(filter)
                .projection(projection)
                .map(mapper::apply)
                .into(partial, (os, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(ImmutableList.copyOf(os));
                });
        return out;
    }

    protected CompletableFuture<Boolean> checkExists(Bson filter) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        this.getCollection()
                .find(filter)
                .first((document, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(document != null);
                });
        return out;
    }

    protected <O> CompletableFuture<O> findFirst(Bson filter, @NotNull Function<Document, O> mapper) {
        return this.findFirst(filter, null, mapper);
    }

    protected <O> CompletableFuture<O> findFirst(Bson filter, Bson projection, @NotNull Function<Document, O> mapper) {
        CompletableFuture<O> out = new CompletableFuture<>();
        //noinspection NullableProblems
        this.getCollection()
                .find(filter)
                .projection(projection)
                .map(mapper::apply)
                .first((o, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                    }
                    out.complete(o);
                });

        return out;
    }

    protected CompletableFuture<Document> insertOne(Document insert) {
        CompletableFuture<Document> out = new CompletableFuture<>();
        this.getCollection()
                .insertOne(insert, (aVoid, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(insert);
                });
        return out;
    }

    public CompletableFuture<Boolean> deleteOne(Bson filter) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        this.getCollection()
                .deleteOne(filter, (deleteResult, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(deleteResult.wasAcknowledged());
                });
        return out;
    }

    public <O> CompletableFuture<List<O>> aggregate(List<Bson> pipeline, Function<Document, O> mapper) {
        CompletableFuture<List<O>> out = new CompletableFuture<>();
        //noinspection NullableProblems
        this.getCollection()
                .aggregate(pipeline)
                .map(mapper::apply)
                .into(new ArrayList<>(), (SingleResultCallback<List<O>>) (os, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(os);
                });
        return out;
    }

    public <O> CompletableFuture<O> aggregateFirst(List<Bson> pipeline, Function<Document, O> mapper) {
        CompletableFuture<O> out = new CompletableFuture<>();
        //noinspection NullableProblems
        this.getCollection()
                .aggregate(pipeline)
                .map(mapper::apply)
                .first((o, throwable) -> {
                    if (throwable != null) {
                        out.completeExceptionally(throwable);
                        return;
                    }

                    out.complete(o);
                });
        return out;
    }

    public CompletableFuture<Long> count(Bson filter) {
        CompletableFuture<Long> out = new CompletableFuture<>();
        this.getCollection().countDocuments(filter, (aLong, throwable) -> {
            if (throwable != null) {
                out.completeExceptionally(throwable);
                return;
            }

            out.complete(aLong);
        });
        return out;
    }

}
