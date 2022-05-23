package com.joinhocus.horus.http.routes.posts.get;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.db.repos.PostsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.http.routes.posts.get.model.AuthorFilter;
import com.joinhocus.horus.http.routes.posts.get.model.GetFeedRequest;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.organization.Organization;
import com.joinhocus.horus.post.Post;
import com.joinhocus.horus.post.PostType;
import com.joinhocus.horus.post.impl.PostWithExtra;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GetFeedHandler implements DefinedTypesWithUserHandler<EmptyRequest> {

    private final JsonObject EMPTY_PAGINATION = new JsonObject();

    {
        EMPTY_PAGINATION.addProperty("pages", 0);
        EMPTY_PAGINATION.addProperty("page", 0);
        EMPTY_PAGINATION.addProperty("hasNext", false);
        EMPTY_PAGINATION.addProperty("hasPages", false);
    }

    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        int page = getIntParam("page", context);
        JsonObject filter = context.queryParam("filter", JsonObject.class)
                .get();
        if (filter == null) {
            filter = new JsonObject();
        }
        JsonArray typesArray = filter.get("types").getAsJsonArray();
        EnumSet<PostType> types = EnumSet.noneOf(PostType.class);
        if (typesArray != null && typesArray.size() != 0) {
            for (JsonElement element : typesArray) {
                String type = element.getAsString();
                PostType actualType = PostType.valueOf(type);
                types.add(actualType);
            }
        }
        if (types.isEmpty()) {
            types = PostType.ALL;
        }

        boolean requiresFilter = true; // if this is true, we're probably getting the main feed so we filter out
        JsonElement author = filter.get("author");
        ObjectId authorId = null;
        if (!author.isJsonNull()) {
            authorId = MongoIds.parseId(author.getAsString());
            if (authorId == null) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("malformed author id"));
            }
        }
        JsonElement organization = filter.get("organization");
        ObjectId orgId = null;
        if (!organization.isJsonNull()) {
            orgId = MongoIds.parseId(organization.getAsString());
            if (orgId == null) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("malformed organization id"));
            }
        }

        if (authorId != null || orgId != null) {
            requiresFilter = false;
        }

        AuthorFilter authorFilter = new AuthorFilter(authorId, orgId);
        return startPipeline(account, new GetFeedRequest(page, types, authorFilter), requiresFilter);
    }

    private CompletableFuture<Response> startPipeline(UserAccount account, GetFeedRequest request, boolean requiresFilter) {
        if (requiresFilter) {
            return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).getFeedOrganizations(account).thenCompose(list -> {
                if (list.isEmpty()) {
                    return wrap(
                            Response.of(Response.Type.OKAY)
                                    .append("posts", new JsonArray())
                                    .append("pagination", EMPTY_PAGINATION)
                    );
                }

                return handleOrganizations(account, request, list);
            });
        }

        return handleOrganizations(account, request, Collections.emptyList());
    }

    private CompletableFuture<Response> handleOrganizations(UserAccount account, GetFeedRequest request, List<Organization> organizationList) {
        return MongoDatabase.getInstance().getRepo(PostsRepo.class).getFeed(
                account,
                organizationList.stream().map(Organization::getId).collect(Collectors.toList()),
                request.getTypes(),
                request.getPage(),
                20,
                request.getAuthorFilter()
        ).thenApply(paginatedPosts -> {
            JsonArray array = new JsonArray();
            for (Post post : paginatedPosts.getData()) {
                array.add(postToJson(post));
            }

            return Response.of(Response.Type.OKAY).append("posts", array).append("pagination", paginatedPosts.getPaginationData());
        });
    }

    private JsonObject postToJson(Post post) {
        JsonObject parent = new JsonObject();
        parent.addProperty("id", post.getId().toHexString());
        UserAccount authorAccount = post.author();
        JsonObject author = new JsonObject();
        author.addProperty("id", authorAccount.getId().toHexString());
        author.addProperty("name", authorAccount.getName());
        author.addProperty("handle", authorAccount.getUsernames().getDisplay());

        JsonObject organization = new JsonObject();
        Organization organizationObject = post.organization();
        organization.addProperty("id", organizationObject.getId().toHexString());
        organization.addProperty("name", organizationObject.getName());
        organization.addProperty("logo", organizationObject.getSettings().getLogo());

        parent.addProperty("content", post.content());
        parent.addProperty("type", post.type().name());
        parent.addProperty("postTime", post.postTime().getTime());

        if (post instanceof PostWithExtra) {
            PostWithExtra extra = (PostWithExtra) post;
            JsonObject extraData = extra.getExtra();
            parent.add("extra", extraData);
        }

        parent.add("organization", organization);
        parent.add("author", author);
        return parent;
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
