package com.joinhocus.horus.db.repos;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.http.routes.posts.get.model.AuthorFilter;
import com.joinhocus.horus.misc.PaginatedList;
import com.joinhocus.horus.organization.Organization;
import com.joinhocus.horus.organization.SimpleOrganization;
import com.joinhocus.horus.post.Post;
import com.joinhocus.horus.post.PostPrivacy;
import com.joinhocus.horus.post.PostType;
import com.joinhocus.horus.post.impl.BasicPost;
import com.joinhocus.horus.post.impl.PostWithExtra;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.GraphLookupOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PostsRepo extends AsyncMongoRepo {

    private final UnwindOptions UNWIND_FIX = new UnwindOptions().preserveNullAndEmptyArrays(true);

    public PostsRepo() {
        super("hocus", "posts");
    }

    @Override
    public void postBoot() {
        createIndexes(Lists.newArrayList(
                new IndexModel(Indexes.ascending("parent"), INDEX_BACKGROUND),
                new IndexModel(Indexes.ascending("organization"), INDEX_BACKGROUND),
                new IndexModel(Indexes.ascending("type"), INDEX_BACKGROUND),
                new IndexModel(Indexes.descending("timestamp"), INDEX_BACKGROUND)
        ));
    }

    public CompletableFuture<ObjectId> createPost(Post post) {
        Document document = new Document()
                .append("_id", post.getId())
                .append("type", post.type() == null ? null : post.type().name())
                .append("content", post.content())
                .append("visibility", post.privacy() == null ? null : post.privacy().name())
                .append("author", post.author().getId())
                .append("organization", post.organization() == null ? null : post.organization().getId())
                .append("timestamp", post.postTime())
                .append("parent", post.parent())
                .append("replyCount", 0)
                .append("likeCount", 0);

        return this.insertOne(document).thenApply(doc -> doc.getObjectId("_id"));
    }

    public CompletableFuture<BasicPost> getById(ObjectId id) {
        return this.aggregateFirst(Lists.newArrayList(
                Aggregates.match(Filters.eq("_id", id)),
                Aggregates.lookup("organizations", "organization", "_id", "organization"),
                Aggregates.unwind("$organization", UNWIND_FIX),
                // inject the author into the parent
                Aggregates.lookup("accounts", "author", "_id", "author"),
                Aggregates.unwind("$author")
        ), document -> {
            Organization organization = null;
            if (document.get("organization") != null) {
                organization = new SimpleOrganization(document.get("organization", Document.class));
            }

            return new BasicPost(
                    document.getObjectId("_id"),
                    document.get("type") != null ? PostType.valueOf(document.getString("type")) : null,
                    document.get("visibility") != null ? PostPrivacy.valueOf(document.getString("visibility")) : null,
                    document.getString("content"),
                    new UserAccount(document.get("author", Document.class)),
                    organization,
                    document.getDate("timestamp"),
                    document.getObjectId("parent"),
                    Lists.newArrayList()
            );
        });
    }

    public CompletableFuture<PostWithExtra> getById(ObjectId id, UserAccount liker) {
        return this.aggregateFirst(Lists.newArrayList(
                Aggregates.match(Filters.eq("_id", id)),
                Aggregates.lookup("organizations", "organization", "_id", "organization"),
                Aggregates.unwind("$organization"),
                // inject the author into the parent
                Aggregates.lookup("accounts", "author", "_id", "author"),
                Aggregates.unwind("$author"),
                Aggregates.lookup("likes", Lists.newArrayList(
                        new Variable<>("post_id", "$_id")
                ), Lists.newArrayList(
                        Aggregates.match(Filters.expr(new Document("$and", Lists.newArrayList(
                                new Document("$eq", Arrays.asList("$liker", liker.getId())),
                                new Document("$eq", Arrays.asList("$post", "$$post_id"))
                        ))))
                ), "liked"),
                Aggregates.unwind("$liked", UNWIND_FIX)
        ), document -> {
            JsonObject extra = new JsonObject();
            extra.addProperty("liked", document.get("liked") != null);
            extra.addProperty("likes", document.getInteger("likeCount", 0));
            extra.addProperty("replies", document.getInteger("replyCount", 0));

            Organization organization = null;
            if (document.get("organization") != null) {
                organization = new SimpleOrganization(document.get("organization", Document.class));
            }

            return new PostWithExtra(
                    document.getObjectId("_id"),
                    document.get("type") != null ?  PostType.valueOf(document.getString("type")) : null,
                    document.get("visibility") != null ? PostPrivacy.valueOf(document.getString("visibility")) : null,
                    document.getString("content"),
                    new UserAccount(document.get("author", Document.class)),
                    organization,
                    document.getDate("timestamp"),
                    document.getObjectId("parent"),
                    Lists.newArrayList(),
                    extra
            );
        });
    }

    // TODO: make this take followed user accounts into account as well
    public CompletableFuture<PaginatedList<PostWithExtra>> getFeed(
            UserAccount liker,
            List<ObjectId> organizations,
            Set<PostType> types,
            int page,
            int limit,
            AuthorFilter authorFilter
    ) {
        List<String> typeNames = types.stream().map(PostType::name).collect(Collectors.toList());
        List<Bson> filters = Lists.newArrayList(
                Filters.eq("parent", null),
                Filters.in("type", typeNames)
        );
        if (!organizations.isEmpty()) {
            filters.add(Filters.in("organization", organizations));
        }

        if (authorFilter != null) {
            if (authorFilter.getAuthor() != null) {
                filters.add(Filters.eq("author", authorFilter.getAuthor()));
            }
            if (authorFilter.getOrganization() != null) {
                filters.add(Filters.eq("organization", authorFilter.getOrganization()));
            }
        }
        Bson filter = Filters.and(filters);

        return this.count(filter).thenCompose(total -> this.aggregate(Lists.newArrayList(
                Aggregates.match(filter),
                Aggregates.sort(Sorts.descending("timestamp")),
                Aggregates.skip(limit * page),
                Aggregates.limit(limit),
                Aggregates.lookup("organizations", "organization", "_id", "organization"),
                Aggregates.unwind("$organization"),
                // inject the author into the parent
                Aggregates.lookup("accounts", "author", "_id", "author"),
                Aggregates.unwind("$author"),
                // check if user liked it
                Aggregates.lookup("likes", Lists.newArrayList(
                        new Variable<>("post_id", "$_id")
                ), Lists.newArrayList(
                        Aggregates.match(Filters.expr(new Document("$and", Lists.newArrayList(
                                new Document("$eq", Arrays.asList("$liker", liker.getId())),
                                new Document("$eq", Arrays.asList("$post", "$$post_id"))
                        ))))
                ), "liked"),
                Aggregates.unwind("$liked", UNWIND_FIX)
        ), document -> {
            JsonObject extra = new JsonObject();
            extra.addProperty("liked", document.get("liked") != null);
            extra.addProperty("likes", document.getInteger("likeCount", 0));
            extra.addProperty("replies", document.getInteger("replyCount", 0));

            Organization organization = null;
            if (document.get("organization") != null) {
                organization = new SimpleOrganization(document.get("organization", Document.class));
            }

            return new PostWithExtra(
                    document.getObjectId("_id"),
                    document.get("type") != null ?  PostType.valueOf(document.getString("type")) : null,
                    document.get("visibility") != null ? PostPrivacy.valueOf(document.getString("visibility")) : null,
                    document.getString("content"),
                    new UserAccount(document.get("author", Document.class)),
                    organization,
                    document.getDate("timestamp"),
                    document.getObjectId("parent"),
                    Lists.newArrayList(),
                    extra
            );
        }).thenApply(posts -> {
            JsonObject pagination = new JsonObject();
            pagination.addProperty("page", page);
            //noinspection IntegerDivisionInFloatingPointContext
            int available = (int) Math.ceil(total / limit);

            pagination.addProperty("pages", available == 0 ? 0 : available + 1);
            pagination.addProperty("hasNext", page < available);
            pagination.addProperty("hasPages", available != 0);

            return new PaginatedList<>(pagination, posts);
        }));
    }


    public CompletableFuture<PostWithExtra> getByIdComplex(
            ObjectId id,
            UserAccount liker
    ) {
        return this.aggregateFirst(Lists.newArrayList(
                // match the parent posts we need to fetch
                Aggregates.match(Filters.eq("_id", id)),
                // inject the organization into the parent
                Aggregates.lookup("organizations", "organization", "_id", "organization"),
                Aggregates.unwind("$organization", UNWIND_FIX),
                // inject the author into the parent
                Aggregates.lookup("accounts", "author", "_id", "author"),
                Aggregates.unwind("$author", UNWIND_FIX),
                // execute a graphLookup on the posts which are considered our children, and their matching children
                Aggregates.graphLookup("posts", "$_id", "_id", "parent", "comments", new GraphLookupOptions().depthField("level")),
                Aggregates.unwind("$comments", UNWIND_FIX),
                // check if user liked it
                Aggregates.lookup("likes", Lists.newArrayList(
                        new Variable<>("post_id", "$_id")
                ), Lists.newArrayList(
                        Aggregates.match(Filters.expr(new Document("$and", Lists.newArrayList(
                                new Document("$eq", Arrays.asList("$liker", liker.getId())),
                                new Document("$eq", Arrays.asList("$post", "$$post_id"))
                        ))))
                ), "liked"),
                Aggregates.unwind("$liked", UNWIND_FIX),
                // inject author into the children
                Aggregates.lookup("accounts", Lists.newArrayList(
                        new Variable<>("author_id", "$comments.author")
                ), Lists.newArrayList(
                        Aggregates.match(Filters.expr(new Document("$eq", Arrays.asList("$_id", "$$author_id"))))
                ), "comments.author"),
                Aggregates.unwind("$comments.author", UNWIND_FIX),
                // inject organization into the children
                Aggregates.lookup("organizations", Lists.newArrayList(
                        new Variable<>("org_id", "$comments.organization")
                ), Lists.newArrayList(
                        Aggregates.match(Filters.expr(new Document("$eq", Arrays.asList("$_id", "$$org_id"))))
                ), "comments.organization"),
                Aggregates.unwind("$comments.organization", UNWIND_FIX),
                Aggregates.lookup("likes", Lists.newArrayList(
                        new Variable<>("post_id", "$comments._id")
                ), Lists.newArrayList(
                        Aggregates.match(Filters.expr(new Document("$and", Lists.newArrayList(
                                new Document("$eq", Arrays.asList("$liker", liker.getId())),
                                new Document("$eq", Arrays.asList("$post", "$$post_id"))
                        ))))
                ), "comments.liked"),
                Aggregates.unwind("$comments.liked", UNWIND_FIX),
                // sort our comments first by depth and then by ascending timestamp (first to last)
                Aggregates.sort(Sorts.orderBy(
                        Sorts.descending("comments.level"),
                        Sorts.ascending("comments.timestamp")
                )),
                // place all of our information so far into the parent document
                new Document("$group", new Document()
                        .append("_id", "$_id")
                        .append("parent", new Document("$first", "$parent"))
                        .append("type", new Document("$first", "$type"))
                        .append("visibility", new Document("$first", "$visibility"))
                        .append("content", new Document("$first", "$content"))
                        .append("organization", new Document("$first", "$organization"))
                        .append("author", new Document("$first", "$author"))
                        .append("timestamp", new Document("$first", "$timestamp"))
                        .append("liked", new Document("$first", "$liked"))
                        .append("likeCount", new Document("$first", "$likeCount"))
                        .append("replyCount", new Document("$first", "$replyCount"))
                        .append("comments", new Document("$push", "$comments"))
                ),
                // get rid of any information we may not need/sensitive data
                Aggregates.project(Projections.include(
                        "_id",
                        "type",
                        "content",
                        "visibility",
                        "author._id",
                        "author.name",
                        "author.user",
                        "organization._id",
                        "organization.name",
                        "organization.settings.logo",
                        "timestamp",
                        "liked",
                        "likeCount",
                        "replyCount",
                        "comments._id",
                        "comments.type",
                        "comments.content",
                        "comments.author._id",
                        "comments.author.name",
                        "comments.author.user",
                        "comments.timestamp",
                        "comments.parent",
                        "comments.organization._id",
                        "comments.organization.name",
                        "comments.organization.settings.logo",
                        "comments.level",
                        "comments.liked",
                        "comments.likeCount",
                        "comments.replyCount"
                )),
                // sort all the children by last to first
                Aggregates.sort(Sorts.descending("comments.timestamp")),
                // and now we inject the comments into the main document, with their children as well
                new Document("$addFields", new Document()
                        .append("comments", new Document()
                                .append("$reduce", new Document()
                                        .append("input", "$comments")
                                        .append("initialValue", new Document()
                                                .append("currentLevel", -1)
                                                .append("currentLevelComments", Collections.emptyList())
                                                .append("previousLevelComments", Collections.emptyList())
                                        )
                                        .append("in", new Document()
                                                .append("$let", new Document()
                                                        .append("vars", new Document()
                                                                .append("prev", new Document()
                                                                        .append("$cond", Lists.newArrayList(
                                                                                new Document().append("$eq", Lists.newArrayList("$$value.currentLevel", "$$this.level")),
                                                                                "$$value.previousLevelComments",
                                                                                "$$value.currentLevelComments"
                                                                        ))
                                                                )
                                                                .append("current", new Document()
                                                                        .append("$cond", Lists.newArrayList(
                                                                                new Document().append("$eq", Lists.newArrayList("$$value.currentLevel", "$$this.level")),
                                                                                "$$value.currentLevelComments",
                                                                                Collections.emptyList()
                                                                        ))
                                                                )
                                                        )
                                                        .append("in", new Document()
                                                                .append("currentLevel", "$$this.level")
                                                                .append("previousLevelComments", "$$prev")
                                                                .append("currentLevelComments", new Document()
                                                                        .append("$concatArrays", Lists.newArrayList(
                                                                                "$$current",
                                                                                Lists.newArrayList(
                                                                                        new Document().append("$mergeObjects", Lists.newArrayList(
                                                                                                "$$this",
                                                                                                new Document().append("comments", new Document("$filter", new Document()
                                                                                                        .append("input", "$$prev")
                                                                                                        .append("as", "comment")
                                                                                                        .append("cond", new Document()
                                                                                                                .append("$eq", Lists.newArrayList(
                                                                                                                        "$$comment.parent",
                                                                                                                        "$$this._id"
                                                                                                                ))
                                                                                                        )
                                                                                                ))
                                                                                        ))
                                                                                )
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                Aggregates.addFields(new Field<>("comments", "$comments.currentLevelComments"))
        ), this::recursive);
    }

    private PostWithExtra recursive(Document document) {
        JsonObject extra = new JsonObject();
        extra.addProperty("liked", document.get("liked") != null);
        extra.addProperty("likes", document.getInteger("likeCount", 0));
        extra.addProperty("replies", document.getInteger("replyCount", 0));

        Organization organization = null;
        if (document.get("organization") != null) {
            organization = new SimpleOrganization(document.get("organization", Document.class));
        }

        PostWithExtra parent = new PostWithExtra(
                document.getObjectId("_id"),
                document.get("type") != null ?  PostType.valueOf(document.getString("type")) : null,
                document.get("visibility") != null ? PostPrivacy.valueOf(document.getString("visibility")) : null,
                document.getString("content"),
                new UserAccount(document.get("author", Document.class)),
                organization,
                document.getDate("timestamp"),
                document.getObjectId("parent"),
                Lists.newArrayList(),
                extra
        );
        //noinspection unchecked
        List<Document> children = (List<Document>) document.get("comments");
        for (Document child : children) {
            if (!child.containsKey("_id")) continue;
            parent.addComment(recursive(child));
        }

        return parent;
    }

    public CompletableFuture<Boolean> incLikes(ObjectId postId, int amount) {
        return this.updateOne(Filters.eq("_id", postId), Updates.inc("likeCount", amount));
    }

    public CompletableFuture<Boolean> incReplies(ObjectId postId, int amount) {
        return this.updateOne(Filters.eq("_id", postId), Updates.inc("replyCount", amount));
    }

    public CompletableFuture<Boolean> checkExists(ObjectId id) {
        return this.checkExists(Filters.eq("_id", id));
    }

    public CompletableFuture<Long> countPostsBy(ObjectId id) {
        return this.count(Filters.and(
                Filters.eq("author", id),
                Filters.eq("parent", null)
        ));
    }

    public CompletableFuture<Long> countCommentsBy(ObjectId id) {
        return this.count(Filters.and(
                Filters.eq("author", id),
                Filters.not(Filters.eq("parent", null))
        ));
    }

    public CompletableFuture<Long> countWithOrg(ObjectId id) {
        return this.count(Filters.and(
                Filters.eq("organization", id),
                Filters.eq("parent", null)
        ));
    }
}
