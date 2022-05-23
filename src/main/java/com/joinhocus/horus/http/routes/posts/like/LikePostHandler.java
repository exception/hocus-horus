package com.joinhocus.horus.http.routes.posts.like;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.activity.ActivityNotification;
import com.joinhocus.horus.account.activity.NotificationType;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.LikesRepo;
import com.joinhocus.horus.db.repos.PostsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.posts.like.model.LikePostRequest;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.misc.follow.EntityType;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LikePostHandler implements DefinedTypesWithUserHandler<LikePostRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends LikePostRequest> validator, Context context, Logger logger) throws Exception {
        decorateWithRateLimit(context, 2, TimeUnit.SECONDS);
        LikePostRequest request = validator
                .check("id", req -> !Strings.isNullOrEmpty(req.getId()), "id cannot be empty")
                .get();

        ObjectId id = MongoIds.parseId(request.getId());
        if (id == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Malformed post id"));
        }
        return startPipeline(account, id);
    }

    private CompletableFuture<Response> startPipeline(UserAccount account, ObjectId id) {
        return MongoDatabase.getInstance().getRepo(LikesRepo.class).hasLiked(account, id).thenCompose(liked -> {
            if (liked) {
                return deleteLike(account, id);
            }

            return like(account, id);
        });
    }

    private CompletableFuture<Response> deleteLike(UserAccount account, ObjectId id) {
        return MongoDatabase.getInstance().getRepo(LikesRepo.class).deleteLike(account, id).thenApply(deleted -> {
            return Response.of(Response.Type.OKAY).append("liked", false);
        });
    }

    private CompletableFuture<Response> like(UserAccount account, ObjectId id) {
        return MongoDatabase.getInstance().getRepo(LikesRepo.class).createLike(account, id).thenCompose(deleted -> {
            return MongoDatabase.getInstance().getRepo(PostsRepo.class).getById(id).thenCompose(post -> {
                List<ObjectId> recipients = new ArrayList<>();
                if (post.author() != null) {
                    if (!post.author().equals(account)) {
                        recipients.add(post.author().getId());
                    }
                }
                if (post.organization() != null) {
                    if (!post.organization().getSeats().containsKey(account.getId())) {
                        recipients.add(post.organization().getId());
                    }
                }

                ActivityNotification notification = new ActivityNotification(
                        new ObjectId(),
                        account.getId(),
                        EntityType.ACCOUNT,
                        recipients,
                        new Date(),
                        NotificationType.LIKE,
                        new Document()
                                .append("post", post.content())
                                .append("postId", post.getId().toHexString())
                );
                return MongoDatabase.getInstance().getRepo(ActivityRepo.class).insertNotification(notification);
            });
        }).thenApply(ignored -> {
            return Response.of(Response.Type.OKAY).append("liked", true);
        });
    }

    @Override
    public Class<? extends LikePostRequest> requestClass() {
        return LikePostRequest.class;
    }
}
