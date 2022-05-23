package com.joinhocus.horus.http.routes.waitlist.validate;

import com.google.common.base.Strings;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.EmailInviteVerificationRepo;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.waitlist.validate.model.EmailValidateWaitListRequest;
import com.joinhocus.horus.misc.MongoIds;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class EmailValidateWaitlistHandler implements DefinedTypesHandler<EmailValidateWaitListRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends EmailValidateWaitListRequest> validator, Context context, Logger logger) throws Exception {
        EmailValidateWaitListRequest request = validator
                .check("code", req -> !Strings.isNullOrEmpty(req.getCode()), "code cannot be empty")
                .get();
        return MongoDatabase.getInstance()
                .getRepo(EmailInviteVerificationRepo.class)
                .getByCode(request.getCode())
                .thenCompose(document -> {
                    if (document == null) {
                        return wrap(Response.of(Response.Type.OKAY).append("accepted", false));
                    }

                    String inviteId = document.getString("inviteId");
                    return runInvitePipeline(inviteId);
                });
    }

    private CompletableFuture<Response> runInvitePipeline(String inviteId) {
        ObjectId id = MongoIds.parseId(inviteId);
        if (id == null) {
            return wrap(Response.of(Response.Type.OKAY).append("accepted", false));
        }
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class)
                .getInviteById(id)
                .thenCompose(document -> {
                    if (document == null) {
                        return wrap(Response.of(Response.Type.OKAY).append("accepted", false));
                    }
                    return wrap(Response.of(Response.Type.OKAY).append("accepted", true).append("code", document.getString("code")));
                });
    }

    @Override
    public Class<? extends EmailValidateWaitListRequest> requestClass() {
        return EmailValidateWaitListRequest.class;
    }
}
