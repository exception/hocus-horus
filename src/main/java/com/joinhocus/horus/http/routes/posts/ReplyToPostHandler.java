package com.joinhocus.horus.http.routes.posts;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.activity.ActivityNotification;
import com.joinhocus.horus.account.activity.NotificationType;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.PostsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.posts.model.ReplyToPostRequest;
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
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

public class ReplyToPostHandler implements DefinedTypesWithUserHandler<ReplyToPostRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends ReplyToPostRequest> validator, Context context, Logger logger) throws Exception {
        ReplyToPostRequest request = validator
                .check("content", req -> !Strings.isNullOrEmpty(req.getContent()), "content cannot be empty")
                .check("content", req -> req.getContent().trim().length() <= 240, "content must be 240 characters at max")
                .check("parent", req -> !Strings.isNullOrEmpty(req.getParent()), "parent cannot be empty")
                .get();

        ObjectId parent = MongoIds.parseId(request.getParent());
        if (parent == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Malformed parent id"));
        }

        return startPipeline(account, request.getContent(), parent);
    }

    private CompletableFuture<Response> startPipeline(UserAccount account, String content, ObjectId parent) {
        return MongoDatabase.getInstance().getRepo(PostsRepo.class).checkExists(parent).thenCompose(exists -> {
            if (!exists) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Parent post no longer exists"));
            }

            return doPostFetch(account, content, parent);
        });
    }

    private CompletableFuture<Response> doPostFetch(UserAccount account, String content, ObjectId parent) {
        return MongoDatabase.getInstance().getRepo(PostsRepo.class).getById(parent).thenCompose(post -> {
            return doReply(account, content, post);
        });
    }

    private CompletableFuture<Response> doReply(UserAccount account, String content, BasicPost parent) {
        BasicPost reply = new BasicPost(
                new ObjectId(),
                null,
                null,
                content,
                account,
                null,
                new Date(),
                parent.getId(),
                Collections.emptyList()
        );

        return MongoDatabase.getInstance().getRepo(PostsRepo.class).createPost(reply).thenCompose(id -> doReplyData(account, reply, parent));
    }

    private CompletableFuture<Response> doReplyData(UserAccount account, BasicPost reply, BasicPost parent) {
        return MongoDatabase.getInstance().getRepo(PostsRepo.class).incReplies(parent.getId(), 1).thenCompose(ignored -> {
            List<ObjectId> receivers = new ArrayList<>();
            if (parent.organization() != null) {
                Organization organization = parent.organization();
                // don't notify if in org
                if (organization.getSeats().containsKey(account.getId())) {
                    return parseMentions(reply);
                }

                receivers.add(parent.organization().getId());
            }
            if (parent.author() != null) {
                if (parent.author().equals(account)) {
                    return parseMentions(reply);
                }
                receivers.add(parent.author().getId());
            }
            ActivityNotification notification = new ActivityNotification(
                    new ObjectId(),
                    account.getId(),
                    EntityType.ACCOUNT,
                    receivers,
                    new Date(),
                    NotificationType.REPLY,
                    new Document()
                        .append("parent", parent.content())
                        .append("post", reply.content())
                        .append("postId", reply.getId().toHexString())
            );

            return MongoDatabase.getInstance().getRepo(ActivityRepo.class).insertNotification(notification).thenCompose(aVoid -> {
                return parseMentions(reply);
            });
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
            return Response.of(Response.Type.OKAY_CREATED).append("id", post.getId().toHexString());
        });
    }

    @Override
    public Class<? extends ReplyToPostRequest> requestClass() {
        return ReplyToPostRequest.class;
    }
}
