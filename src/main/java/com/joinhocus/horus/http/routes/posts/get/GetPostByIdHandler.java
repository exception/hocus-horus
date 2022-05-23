package com.joinhocus.horus.http.routes.posts.get;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.PostsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.post.PostUtil;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GetPostByIdHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String id = context.queryParam("id");
        boolean complex = getBooleanParam("full", context);
        if (Strings.isNullOrEmpty(id)) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("id cannot be empty"));
        }
        ObjectId mongoId = MongoIds.parseId(id);
        if (mongoId == null) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("malformed post id"));
        }

        if (complex) {
            return getFullPost(mongoId, account);
        }

        return getSimplePost(mongoId, account);
    }

    private CompletableFuture<Response> getFullPost(ObjectId id, UserAccount requester) {
        return MongoDatabase.getInstance().getRepo(PostsRepo.class).getByIdComplex(id, requester).thenApply(post -> {
            if (post == null) {
                return Response.of(Response.Type.NOT_FOUND);
            }

            return Response.of(Response.Type.OKAY).append("post", PostUtil.recursive(post));
        });
    }

    private CompletableFuture<Response> getSimplePost(ObjectId id, UserAccount requester) {
        return MongoDatabase.getInstance().getRepo(PostsRepo.class).getById(id, requester).thenApply(post -> {
            if (post == null) {
                return Response.of(Response.Type.NOT_FOUND);
            }

            return Response.of(Response.Type.OKAY).append("post", PostUtil.recursive(post));
        });
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
