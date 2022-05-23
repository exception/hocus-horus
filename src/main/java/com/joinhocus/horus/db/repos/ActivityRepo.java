package com.joinhocus.horus.db.repos;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.activity.ActivityNotification;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.misc.PaginatedList;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ActivityRepo extends AsyncMongoRepo {
    public ActivityRepo() {
        super("hocus", "activity");
    }

    public CompletableFuture<Void> insertNotification(ActivityNotification notification) {
        if (notification.getReceivers().isEmpty()) {
            logger.warn("Skipping insert for notification w/o receivers {}", notification);
            return CompletableFuture.completedFuture(null);
        }

        Document document = new Document()
                .append("actor", notification.getActor())
                .append("entity", notification.getEntityType().name())
                .append("receivers", notification.getReceivers())
                .append("when", notification.getWhen())
                .append("type", notification.getNotificationType().name())
                .append("extra", notification.getExtra());

        return this.insertOne(document).thenApply(doc -> null);
    }

    public CompletableFuture<PaginatedList<ActivityNotification>> getActivity(
            ObjectId requester,
            List<ObjectId> receivers,
            int page,
            int limit
    ) {
        Bson filter = Filters.in("receivers", receivers);

        return this.count(filter).thenCompose(total -> this.aggregate(Lists.newArrayList(
                Aggregates.match(filter),
                Aggregates.sort(Sorts.descending("when")),
                Aggregates.project(Projections.fields(
                        Projections.include("_id", "actor", "entity", "receivers", "when", "sentence", "type", "extra"),
                        Projections.computed("wasSeen", new Document("$in", Lists.newArrayList(
                                requester,
                                new Document("$ifNull", Lists.newArrayList("$seen", Collections.emptyList()))
                        )))
                )),
                Aggregates.sort(Sorts.ascending("wasSeen")),
                Aggregates.skip(limit * page),
                Aggregates.limit(limit)
        ), ActivityNotification::new).thenApply(notifications -> {
            JsonObject pagination = new JsonObject();
            pagination.addProperty("page", page);
            //noinspection IntegerDivisionInFloatingPointContext
            int available = (int) Math.ceil(total / limit);

            pagination.addProperty("pages", available == 0 ? 0 : available + 1);
            pagination.addProperty("hasNext", page < available);
            pagination.addProperty("hasPages", available != 0);

            return new PaginatedList<>(pagination, notifications);
        }));
    }

    public CompletableFuture<Long> countActivity(
            ObjectId requester,
            List<ObjectId> receivers
    ) {
        Bson filter = Filters.and(
                Filters.in("receivers", receivers),
                Filters.nin("seen", requester)
        );

        return this.count(filter);
    }

    public CompletableFuture<Boolean> markSeen(List<ObjectId> ids, ObjectId viewer) {
        return this.updateMany(Filters.in("_id", ids), Updates.addToSet("seen", viewer));
    }
}
