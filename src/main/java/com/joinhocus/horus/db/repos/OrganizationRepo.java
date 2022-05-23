package com.joinhocus.horus.db.repos;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.misc.Spaces;
import com.joinhocus.horus.misc.follow.Followable;
import com.joinhocus.horus.organization.Organization;
import com.joinhocus.horus.organization.OrganizationRole;
import com.joinhocus.horus.organization.SimpleOrganization;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import io.javalin.http.UploadedFile;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

public class OrganizationRepo extends AsyncMongoRepo {
    public OrganizationRepo() {
        super("hocus", "organizations");
    }

    @Override
    public void postBoot() {
        createIndexes(Lists.newArrayList(
                new IndexModel(Indexes.ascending("name.handle"), INDEX_BACKGROUND),
                new IndexModel(Indexes.ascending("seats.id"), INDEX_BACKGROUND)
        ));
    }

    public CompletableFuture<Document> createOrganization(Organization organization, UserAccount creator) {
        Document name = new Document()
                .append("display", organization.getName())
                .append("handle", organization.getHandle());

        List<Document> seats = new ArrayList<>();
        seats.add(new Document().
                append("id", creator.getId()).
                append("role", "CREATOR")
        );

        Document out = new Document()
                .append("name", name)
                .append("settings", new Document())
                .append("creator", creator.getId())
                .append("createdAt", System.currentTimeMillis())
                .append("seats", seats);

        return this.insertOne(out);
    }

    public CompletableFuture<Boolean> isHandleAvailable(String handle) {
        return this.checkExists(Filters.eq("name.handle", handle.toLowerCase()));
    }

    public CompletableFuture<Boolean> setOrganizationLogo(ObjectId organizationId, UploadedFile file) {
        CompletableFuture<String> put = new CompletableFuture<>();
        ForkJoinPool.commonPool().submit(() -> {
            try {
                String url = Spaces.uploadImage(file, "orgLogos", organizationId.toHexString());
                put.complete(url);
            } catch (Exception e) {
                put.completeExceptionally(e);
            }
        });

        return put.thenCompose(url -> {
            return this.updateOne(Filters.eq("_id", organizationId), Updates.set("settings.logo", url));
        });
    }

    public CompletableFuture<Organization> getById(ObjectId objectId) {
        return this.findFirst(Filters.eq("_id", objectId), SimpleOrganization::new);
    }

    public CompletableFuture<ImmutableList<Organization>> getByIds(List<ObjectId> ids) {
        return this.find(Filters.in("_id", ids), null, SimpleOrganization::new);
    }

    public CompletableFuture<Organization> getByHandle(String handle) {
        return this.findFirst(Filters.eq("name.handle", handle), SimpleOrganization::new);
    }

    public CompletableFuture<Boolean> isMemberOf(ObjectId organizationId, ObjectId account) {
        return this.findFirst(Filters.and(
                Filters.eq("_id", organizationId),
                Filters.elemMatch("seats", Filters.eq("id", account))
        ), Objects::nonNull);
    }

    public CompletableFuture<ImmutableList<Organization>> getOrganizations(ObjectId member) {
        return this.find(
                Filters.elemMatch("seats", Filters.eq("id", member)),
                Projections.exclude("seats"),
                SimpleOrganization::new
        );
    }

    public CompletableFuture<ImmutableList<Organization>> getFeedOrganizations(Followable member) {
        return CompletableFutures.combine(
                getOrganizations(member.getId()),
                MongoDatabase.getInstance().getRepo(FollowsRepo.class).getFollowedOrganizations(member),
                (resA, resB) -> ImmutableList.<Organization>builder()
                        .addAll(resA)
                        .addAll(resB)
                        .build()
        ).toCompletableFuture();
    }

    public CompletableFuture<ImmutableList<JsonObject>> searchByHandle(String username) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(username), Pattern.CASE_INSENSITIVE);
        return this.find(Filters.regex("name.handle", pattern), Projections.fields(
                Projections.excludeId(),
                Projections.include("name"),
                Projections.include("settings.logo")
        ), doc -> {
            JsonObject object = new JsonObject();
            Document name = doc.get("name", Document.class);
            Document settings = doc.get("settings", Document.class);
            object.addProperty("handle", name.getString("handle"));
            object.addProperty("name", name.getString("display"));
            object.addProperty("avatar", settings.getString("logo"));
            return object;
        });
    }

    public CompletableFuture<ImmutableList<ObjectId>> getAllByHandles(List<String> handles) {
        return this.find(Filters.in("name.handle", handles), null, doc -> doc.getObjectId("_id"));
    }

    public CompletableFuture<Void> addSeat(ObjectId organizationId, UserAccount account, OrganizationRole role) {
        return this.updateOne(Filters.eq("_id", organizationId), Updates.addToSet("seats", new Document()
                .append("id", account.getId())
                .append("role", role.name())
        )).thenApply(ignored -> null);
    }
}
