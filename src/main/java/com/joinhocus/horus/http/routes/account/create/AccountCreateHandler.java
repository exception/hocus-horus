package com.joinhocus.horus.http.routes.account.create;

import com.google.common.base.Strings;
import com.joinhocus.horus.account.AccountAuth;
import com.joinhocus.horus.account.AccountStatus;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.account.UserNames;
import com.joinhocus.horus.account.activity.ActivityNotification;
import com.joinhocus.horus.account.activity.NotificationType;
import com.joinhocus.horus.account.invite.Invite;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.FollowsRepo;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.db.repos.WaitListRepo;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.routes.account.create.model.CreateAccountRequest;
import com.joinhocus.horus.misc.HandlesUtil;
import com.joinhocus.horus.misc.MongoIds;
import com.joinhocus.horus.misc.follow.EntityType;
import com.joinhocus.horus.organization.OrganizationRole;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class AccountCreateHandler implements DefinedTypesHandler<CreateAccountRequest> {
    @Override
    public CompletableFuture<Response> handle(BodyValidator<? extends CreateAccountRequest> validator, Context context, Logger logger) throws Exception {
        CreateAccountRequest request = validator
                .check("email", req -> !Strings.isNullOrEmpty(req.getEmail()), "email cannot be empty")
                .check("email", req -> AccountAuth.isValidEmail(req.getEmail()), "email must be valid")
                .check("username", req -> !Strings.isNullOrEmpty(req.getUsername()), "username cannot be empty")
                .check("username", req -> UserNames.NAME_PATTERN.matcher(req.getUsername()).find(), "username too long or contains invalid characters")
                .check("password", req -> !Strings.isNullOrEmpty(req.getPassword()), "password cannot be empty")
                .check("name", req -> !Strings.isNullOrEmpty(req.getName()), "name cannot be empty")
                .check("inviteCode", req -> !Strings.isNullOrEmpty(req.getInviteCode()), "inviteCode cannot be empty")
                .get();

        return MongoDatabase.getInstance().getRepo(InvitesRepo.class).getInviteByCode(request.getInviteCode())
                .thenCompose(invite -> {
                    if (invite == null) {
                        return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("It seems like the invite you provided was already claimed or invalid!"));
                    }

                    if (invite.isWasClaimed()) {
                        return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("It seems like the invite you provided was already claimed or invalid!"));
                    }

                    return runPipeline(request, invite);
                });
    }

    private CompletableFuture<Response> runPipeline(CreateAccountRequest request, Invite invite) {
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class)
                .checkAccount(request)
                .thenCompose(status -> {
                    if (status != AccountStatus.AVAILABLE && status != null) {
                        return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("Could not create account, " + status.getMessage()));
                    }

                    return checkNameAvailability(request, invite);
                });
    }

    private CompletableFuture<Response> checkNameAvailability(CreateAccountRequest request, Invite invite) {
        return HandlesUtil.isHandleAvailable(request.getUsername()).thenCompose(available -> {
            if (!available) {
                return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("That handle is not available"));
            }

            return createAccount(request, invite);
        });
    }

    private CompletableFuture<Response> createAccount(CreateAccountRequest request, Invite invite) {
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).createAccount(request).thenCompose(account -> {
            return CompletableFuture.allOf(
                    MongoDatabase.getInstance().getRepo(WaitListRepo.class).consume(request.getInviteCode()),
                    MongoDatabase.getInstance().getRepo(InvitesRepo.class).claim(request.getInviteCode())
            ).thenApply(aVoid -> account);
        }).thenCompose(account -> {
            return handleRest(account, invite);
        });
    }

    private CompletableFuture<Response> handleRest(UserAccount account, Invite invite) {
        if (invite.getOrgId() != null) {
            ObjectId orgId = MongoIds.parseId(invite.getOrgId());
            if (orgId != null) {
                return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).addSeat(orgId, account, OrganizationRole.MEMBER)
                        .thenCompose(ignored -> {
                            return doFollow(account, invite.getInviter());
                        });
            }
        }

        return doFollow(account, invite.getInviter());
    }

    private CompletableFuture<Response> doFollow(UserAccount account, ObjectId id) {
        if (id == null) {
            return wrap(Response.of(Response.Type.OKAY_CREATED).append("token", AccountAuth.getToken(account)));
        }
        return MongoDatabase.getInstance().getRepo(FollowsRepo.class).follow(account, id, EntityType.ACCOUNT).thenCompose(ignored -> {
            ActivityNotification notification = new ActivityNotification(
                    new ObjectId(),
                    account.getId(),
                    EntityType.ACCOUNT,
                    Collections.singletonList(id),
                    new Date(),
                    NotificationType.FOLLOW,
                    new Document()
                            .append("followed", account.getId().toHexString())
                            .append("handle", account.getUsernames().getDisplay())
            );

            return MongoDatabase.getInstance().getRepo(ActivityRepo.class).insertNotification(notification).thenApply(aVoid -> {
                return Response.of(Response.Type.OKAY_CREATED).append("token", AccountAuth.getToken(account));
            });
        });
    }

    @Override
    public Class<? extends CreateAccountRequest> requestClass() {
        return CreateAccountRequest.class;
    }
}
