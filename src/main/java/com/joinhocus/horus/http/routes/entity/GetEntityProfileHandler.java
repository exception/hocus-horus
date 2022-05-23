package com.joinhocus.horus.http.routes.entity;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.FollowsRepo;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.db.repos.PostsRepo;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import com.joinhocus.horus.misc.CompletableFutures;
import com.joinhocus.horus.organization.Organization;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GetEntityProfileHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        String handle = context.queryParam("handle");
        if (Strings.isNullOrEmpty(handle)) {
            return wrap(Response.of(Response.Type.BAD_REQUEST).setMessage("handle cannot be empty"));
        }

        return startPipeline(handle.toLowerCase());
    }

    private CompletableFuture<Response> startPipeline(String handle) {
        // first we check if it's an account
        return MongoDatabase.getInstance().getRepo(AccountsRepo.class).findByHandle(handle).thenCompose(account -> {
            if (account == null) {
                return handleFetchAsOrg(handle);
            }

            return handleAsAccount(account);
        });
    }

    private CompletableFuture<Response> handleFetchAsOrg(String handle) {
        return MongoDatabase.getInstance().getRepo(OrganizationRepo.class).getByHandle(handle).thenCompose(org -> {
            if (org == null) {
                return wrap(Response.of(Response.Type.NOT_FOUND));
            }

            return handleAsOrg(org);
        });
    }

    private CompletableFuture<Response> handleAsAccount(UserAccount account) {
        return CompletableFutures.combine(
                MongoDatabase.getInstance().getRepo(FollowsRepo.class).getFollowers(account),
                MongoDatabase.getInstance().getRepo(PostsRepo.class).countPostsBy(account.getId()),
                MongoDatabase.getInstance().getRepo(PostsRepo.class).countCommentsBy(account.getId()),
                MongoDatabase.getInstance().getRepo(OrganizationRepo.class).getOrganizations(account.getId()),
                (followers, posts, comments, organizations) -> {
                    JsonObject stats = new JsonObject();
                    stats.addProperty("followers", followers);
                    stats.addProperty("posts", posts);
                    stats.addProperty("comments", comments);

                    JsonObject user = new JsonObject();
                    user.addProperty("id", account.getId().toHexString());
                    user.addProperty("name", account.getName());
                    user.addProperty("handle", account.getUsernames().getDisplay());
                    user.addProperty("avatar", account.getAvatar());
                    user.addProperty("type", account.getAccountType().name());

                    JsonArray orgs = new JsonArray();
                    for (Organization organization : organizations) {
                        JsonObject object = new JsonObject();
                        object.addProperty("id", organization.getId().toHexString());
                        object.addProperty("name", organization.getName());
                        object.addProperty("handle", organization.getHandle());

                        JsonObject settings = new JsonObject();
                        settings.addProperty("logo", organization.getSettings().getLogo());
                        object.add("settings", settings);

                        orgs.add(object);
                    }

                    JsonObject profile = new JsonObject();
                    profile.add("stats", stats);
                    profile.add("info", user);
                    profile.add("organizations", orgs);
                    profile.addProperty("type", "ACCOUNT");

                    return Response.of(Response.Type.OKAY).append("profile", profile);
                }
        ).toCompletableFuture();
    }

    private CompletableFuture<Response> handleAsOrg(Organization organization) {
        return CompletableFutures.unwrap(
                CompletableFutures.combine(
                        MongoDatabase.getInstance().getRepo(FollowsRepo.class).getFollowers(organization),
                        MongoDatabase.getInstance().getRepo(PostsRepo.class).countWithOrg(organization.getId()),
                        (followers, posts) -> {
                            JsonObject stats = new JsonObject();
                            stats.addProperty("followers", followers);
                            stats.addProperty("posts", posts);

                            JsonObject data = new JsonObject();
                            data.addProperty("id", organization.getId().toHexString());
                            data.addProperty("name", organization.getName());
                            data.addProperty("handle", organization.getHandle());
                            data.addProperty("logo", organization.getSettings().getLogo());

                            return organization.getUserSeats().thenApply(seats -> {
                                JsonArray members = new JsonArray();
                                for (UserAccount account : seats.keySet()) {
                                    JsonObject user = new JsonObject();
                                    user.addProperty("name", account.getName());
                                    user.addProperty("handle", account.getUsernames().getDisplay());
                                    user.addProperty("avatar", account.getAvatar());

                                    members.add(user);
                                }

                                JsonObject profile = new JsonObject();
                                profile.add("stats", stats);
                                profile.add("info", data);
                                profile.add("members", members);
                                profile.addProperty("type", "ORGANIZATION");

                                return Response.of(Response.Type.OKAY).append("profile", profile);
                            });
                        }
                ).toCompletableFuture()
        ).toCompletableFuture();
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
