package com.joinhocus.horus.http.routes.posts;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.activity.ActivityNotification;
import com.joinhocus.horus.account.activity.NotificationType;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.db.repos.PostsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.posts.model.CreatePostRequest;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.misc.HandlesUtil;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.misc.follow.EntityType;
import com.joinhocus.horus.organization.Organization;
import com.joinhocus.horus.post.Post;
import com.joinhocus.horus.post.impl.BasicPost;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

public class CreatePostHandler implements DefinedTypesWithUserHandler<CreatePostRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends CreatePostRequest> validator, Context context, Logger logger) throws Exception {
        CreatePostRequest request = validator
                .check("content", req -> !Strings.isNullOrEmpty(req.getContent()), "content cannot be empty")
                .check("content", req -> req.getContent().trim().length() <= 240, "content must be 240 characters at max")
                .check("organizationId", req -> !Strings.isNullOrEmpty(req.getOrganizationId()), "organizationId cannot be empty")
                .check("type", req -> !Objects.isNull(req.getType()), "type cannot be empty")
                .check("privacy", req -> !Objects.isNull(req.getPrivacy()), "privacy cannot be empty")
                .get();

        ObjectId organizationId = MongoIds.parseId(request.getOrganizationId());
        if (organizationId == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("organizationId was not valid"));
        }

        return runPipeline(request, account, organizationId);
    }

    private CompletableFuture<Response> runPipeline(CreatePostRequest request, UserAccount account, ObjectId organizationId) {
        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class)
                .isMemberOf(organizationId, account.getId())
                .thenCompose(isMember -> {
                    if (!isMember) {
                        return wrap(Response.of(Response.Type.UNAUTHORIZED));
                    }
                    return runOrganization(request, account, organizationId);
                });
    }

    private CompletableFuture<Response> runOrganization(CreatePostRequest request, UserAccount account, ObjectId organization) {
        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class)
                .getById(organization)
                .thenCompose(org -> {
                    if (org == null) {
                        return wrap(Response.of(Response.Type.BAD_REQUEST));
                    }

                    return createPost(request, account, org);
                });
    }

    private CompletableFuture<Response> createPost(CreatePostRequest request, UserAccount account, Organization organization) {
        Post post = new BasicPost(
                new ObjectId(),
                request.getType(),
                request.getPrivacy(),
                request.getContent(),
                account,
                organization,
                new Date(),
                null,
                Collections.emptyList()
        );

        return MongoDatabase.getInstance().getRepo(PostsRepo.class).createPost(post).thenCompose(id -> {
            if (id != null) {
                return parseMentions(post);
            }

            return CompletableFutures.failedFuture(new IllegalStateException("Failed to create post"));
        });
    }

    private CompletableFuture<Response> parseMentions(Post post) {
        String content = post.content();
        Matcher matcher = HandlesUtil.TAG_PATTERN.matcher(content);
        List<String> handles = new ArrayList<>();
        while (matcher.find()) {
            String match = matcher.group();
            match = match.replace("@", "").replace("[", "").replace("]", "");
            handles.add(match.toLowerCase());
        }

        return HandlesUtil.getEntitiesBy(handles).thenCompose(entities -> {
            if (post.organization() != null) {
                entities.remove(post.organization().getId());
            }
            entities.remove(post.author().getId());

            ActivityNotification notification = new ActivityNotification(
                    new ObjectId(),
                    post.author().getId(),
                    EntityType.ACCOUNT,
                    entities,
                    new Date(),
                    NotificationType.MENTION,
                    new Document()
                        .append("postId", post.getId().toHexString())
                        .append("post", post.content())
            );

            return MongoDatabase.getInstance().getRepo(ActivityRepo.class).insertNotification(notification);
        }).thenApply(ignored -> {
            return Response.of(Response.Type.OKAY).append("id", post.getId().toHexString());
        });
    }

    @Override
    public Class<? extends CreatePostRequest> requestClass() {
        return CreatePostRequest.class;
    }
}
