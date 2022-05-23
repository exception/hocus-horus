package com.joinhocus.horus.http.routes.account.get;

import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.http.DefinedTypesWithUserHandler;
import com.joinhocus.horus.http.Response;
import com.joinhocus.horus.http.model.EmptyRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.Context;
import org.bson.Document;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GetAccountHandler implements DefinedTypesWithUserHandler<EmptyRequest> {
    @Override
    public CompletableFuture<Response> handle(UserAccount account, BodyValidator<? extends EmptyRequest> validator, Context context, Logger logger) throws Exception {
        JsonObject object = new JsonObject();

        object.addProperty("id", account.getId().toHexString());
        object.addProperty("handle", account.getUsernames().getDisplay());
        object.addProperty("name", account.getName());
        object.addProperty("finishedSetup", account.isFinishedAccountSetup());
        object.addProperty("email", account.getEmail());

        Document extra = account.getExtra();
        JsonObject extraInfo = new JsonObject();
        if (extra != null) {
            int invites = extra.getInteger("invites", 0);
            extraInfo.addProperty("invites", invites);
        }

        object.add("extra", extraInfo);
        return wrap(Response.of(Response.Type.OKAY).append("account", object));
    }

    @Override
    public Class<? extends EmptyRequest> requestClass() {
        return EmptyRequest.class;
    }
}
