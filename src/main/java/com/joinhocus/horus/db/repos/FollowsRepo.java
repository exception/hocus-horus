package com.joinhocus.horus.db.repos;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.misc.follow.EntityType;
import com.joinhocus.horus.misc.follow.Followable;
import com.joinhocus.horus.organization.Organization;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class FollowsRepo extends AsyncMongoRepo {
    public FollowsRepo() {
        super("hocus", "follows");
    }

    @Override
    public void postBoot() {
        this.createIndexes(Lists.newArrayList(
                new IndexModel(Indexes.descending("follower"), INDEX_BACKGROUND),
                new IndexModel(Indexes.descending("followee"), INDEX_BACKGROUND)
        ));
    }

    public CompletableFuture<Boolean> follows(Followable follower, ObjectId followee) {
        return this.checkExists(Filters.and(
                Filters.eq("follower", follower.getId()),
                Filters.eq("followee", followee)
        ));
    }

    public CompletableFuture<Void> follow(Followable follower, ObjectId followee, EntityType type) {
        return this.insertOne(
                new Document()
                        .append("follower", follower.getId())
                        .append("followee", followee)
                        .append("since", new Date())
                        .append("kind", type.name())
        ).thenApply(doc -> null);
    }

    public CompletableFuture<ImmutableList<Organization>> getFollowedOrganizations(Followable follower) {
        return this.find(
                Filters.and(
                        Filters.eq("follower", follower.getId()),
                        Filters.eq("kind", EntityType.ORGANIZATION.name())
                ),
                Projections.include("followee"),
                doc -> doc.getObjectId("followee")
        ).thenCompose(objectIds -> MongoDatabase.getInstance().getRepo(OrganizationRepo.class).getByIds(objectIds));
    }

    public CompletableFuture<Void> unfollow(Followable follower, ObjectId followee) {
        return this.deleteOne(
                Filters.and(
                        Filters.eq("follower", follower.getId()),
                        Filters.eq("followee", followee)
                )
        ).thenApply(doc -> null);
    }

    public CompletableFuture<Long> getFollowers(Followable account) {
        return this.count(Filters.eq("followee", account.getId()));
    }
}
