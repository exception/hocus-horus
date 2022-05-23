package com.joinhocus.horus.http;

import com.google.common.base.Preconditions;
import com.joinhocus.horus.http.model.EmptyRequest;
import io.javalin.core.validation.BodyValidator;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpResponseException;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.util.RateLimit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Defines a single structured HTTP handler for a specific Request type
 * all responses are handled as JSON automatically, and all the async handling is done
 * for us by Javalin
 * @param <Request> a GSON decoded request, validated by {@link BodyValidator}
 */
public interface DefinedTypesHandler<Request> extends Handler {

    CompletableFuture<Response> handle(BodyValidator<? extends Request> validator, Context context, Logger logger) throws Exception;

    default CompletableFuture<Response> wrap(Response response) {
        return CompletableFuture.completedFuture(response);
    }

    default void decorateWithRateLimit(Context context, int numReq, TimeUnit unit) {
        // todo replace with our own version which allows us to specify better parameters
        // and uses redis because we run as stateless services
        new RateLimit(context).requestPerTimeUnit(numReq, unit);
    }

    @Override
    default void handle(@NotNull Context context) throws Exception {
        // each handler gets it's own logger
        Logger logger = LoggerFactory.getLogger(getClass());
        CompletableFuture<Response> response;
        if (requestClass().equals(EmptyRequest.class)) {
            response = handle(null, context, logger);
        } else {
            Request request = context.bodyAsClass(requestClass());
            Preconditions.checkNotNull(request);
            response = handle(context.bodyValidator(requestClass()), context, logger);
        }
        if (response == null) {
            context.status(500);
            context.json(new InternalServerErrorResponse());
            return;
        }
        // context#result runs async so easy-peasy async handling i hope?
        context.result(response.thenApply((object) -> {
            if (object == null) {
                throw new BadRequestResponse();
            }

            context.status(object.getCode());
            return object.toJSON().toString();
        }).exceptionally(err -> {
            if (err.getCause() instanceof HttpResponseException) {
                HttpResponseException http = (HttpResponseException) err.getCause();
                context.json(http);
                context.status(http.getStatus());
                return null;
            }
            logger.error("", err);
            throw new InternalServerErrorResponse(err.getMessage());
        }));
    }

    Class<? extends Request> requestClass();

    default int getIntParam(String param, Context context) {
        try {
            String value = context.queryParam(param);
            if (value == null) {
                throw new BadRequestResponse(param + " may not be empty");
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new BadRequestResponse(param + " must be a valid integer");
        }
    }

    default boolean getBooleanParam(String param, Context context) {
        try {
            String value = context.queryParam(param);
            if (value == null) return false;

            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            throw new BadRequestResponse(param + " must be a valid boolean");
        }
    }
}
