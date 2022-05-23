package com.joinhocus.horus.http.routes.account.setup;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.Document;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GetUserInviterInfo implements DefinedTypesWithUserHandler<EmptyRequest> {

    private final Response HORRIS_INVITE = Response.of(Response.Type.OKAY).append("inviter", "horris").append("org", "Hocus");

    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        Document document = account.getDocument();
        String claimedCode = document.getString("acceptedCode");
        if (Strings.isNullOrEmpty(claimedCode)) {
            return wrap(HORRIS_INVITE);
        }
        return runPipeline(claimedCode);
    }

    private CompletableFuture<Response> runPipeline(String code) {
        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).getInviteByCode(code).thenCompose(invite -> {
            if (invite == null) {
                return wrap(HORRIS_INVITE);
            }
            if (invite.getInviter() == null) {
                return wrap(HORRIS_INVITE);
            }
            return runSecondary(invite);
        });
    }

    private CompletableFuture<Response> runSecondary(Invite invite) {
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).findById(invite.getInviter())
                .thenCompose(account -> {
                    if (account == null) {
                        return wrap(HORRIS_INVITE);
                    }

                    return getOrganizationInfo(invite, account);
                });
    }

    private CompletableFuture<Response> getOrganizationInfo(Invite invite, UserAccount inviter) {
        // TODO: do org stuff
        return wrap(Response.of(Response.Type.OKAY).append("inviter", inviter.getUsernames().getDisplay()).append("org", invite.getOrgId()));
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
