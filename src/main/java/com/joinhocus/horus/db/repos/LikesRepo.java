package com.joinhocus.horus.db.repos;

import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.AsyncMongoRepo;
import com.joinhocus.horus.db.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.concurrent.CompletableFuture;

public class LikesRepo extends AsyncMongoRepo {
    public LikesRepo() {
        super("hocus", "likes");
    }

    public CompletableFuture<Boolean> hasLiked(UserAccount liker, ObjectId postId) {
        return this.checkExists(Filters.and(
                Filters.eq("liker", liker.getId()),
                Filters.eq("post", postId)
        ));
    }

    public CompletableFuture<Boolean> createLike(UserAccount liker, ObjectId postId) {
        return this.insertOne(
                new Document()
                    .append("liker", liker.getId())
                    .append("post", postId)
        ).thenApply(ignored -> null).thenCompose(aVoid -> {
            return MongoDatabase.getInstance().getRepo(PostsRepo.class).incLikes(postId, 1);
        });
    }

    public CompletableFuture<Boolean> deleteLike(UserAccount liker, ObjectId postId) {
        return this.deleteOne(Filters.and(
                Filters.eq("liker", liker.getId()),
                Filters.eq("post", postId)
        )).thenCompose(ignored -> {
            return MongoDatabase.getInstance().getRepo(PostsRepo.class).incLikes(postId, -1);
        });
    }
}
