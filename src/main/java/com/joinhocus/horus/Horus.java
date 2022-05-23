package com.joinhocus.horus;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.http.DefinedTypesHandler;
import com.joinhocus.horus.http.routes.account.changepw.ChangePasswordHandler;
import com.joinhocus.horus.http.routes.account.code.ValidateInviteCodeHandler;
import com.joinhocus.horus.http.routes.account.create.AccountCreateHandler;
import com.joinhocus.horus.http.routes.account.create.ValidateInviteTwitterComboHandler;
import com.joinhocus.horus.http.routes.account.forgotpw.RequestPasswordResetHandler;
import com.joinhocus.horus.http.routes.account.forgotpw.ResetPasswordHandler;
import com.joinhocus.horus.http.routes.account.get.CheckAccountUsernameAvailabilityHandler;
import com.joinhocus.horus.http.routes.account.get.GetAccountHandler;
import com.joinhocus.horus.http.routes.account.login.AccountLoginHandler;
import com.joinhocus.horus.http.routes.account.settings.UpdateAccountAvatarHandler;
import com.joinhocus.horus.http.routes.account.setup.GetUserInviterInfo;
import com.joinhocus.horus.http.routes.account.setup.SetupAccountHandler;
import com.joinhocus.horus.http.routes.activity.GetActivityCountHandler;
import com.joinhocus.horus.http.routes.activity.GetActivityFeedHandler;
import com.joinhocus.horus.http.routes.entity.FollowEntityHandler;
import com.joinhocus.horus.http.routes.entity.GetEntityProfileHandler;
import com.joinhocus.horus.http.routes.entity.GetFollowsEntityHandler;
import com.joinhocus.horus.http.routes.entity.SearchByHandleHandler;
import com.joinhocus.horus.http.routes.invites.create.CreateInviteHandler;
import com.joinhocus.horus.http.routes.invites.get.GetCreatedInvitesHandler;
import com.joinhocus.horus.http.routes.invites.token.SendVerificationEmailHandler;
import com.joinhocus.horus.http.routes.invites.validate.ValidateInviteHandler;
import com.joinhocus.horus.http.routes.organization.create.CreateOrganizationHandler;
import com.joinhocus.horus.http.routes.organization.getbyuser.GetUserOrganizationsHandler;
import com.joinhocus.horus.http.routes.organization.settings.logo.UpdateOrganizationLogoHandler;
import com.joinhocus.horus.http.routes.posts.CreatePostHandler;
import com.joinhocus.horus.http.routes.posts.ReplyToPostHandler;
import com.joinhocus.horus.http.routes.posts.get.GetFeedHandler;
import com.joinhocus.horus.http.routes.posts.get.GetPostByIdHandler;
import com.joinhocus.horus.http.routes.posts.like.LikePostHandler;
import com.joinhocus.horus.http.routes.twitter.info.TwitterInfoHandler;
import com.joinhocus.horus.http.routes.waitlist.join.JoinWaitlistTwitterHandler;
import com.joinhocus.horus.http.routes.waitlist.register.twitter.RegisterWithTwitterHandler;
import com.joinhocus.horus.http.routes.waitlist.validate.EmailValidateWaitlistHandler;
import com.joinhocus.horus.http.routes.waitlist.validate.TwitterValidateWaitListHandler;
import com.joinhocus.horus.http.routes.waitlist.verify.VerifyWaitlistTwitterHandler;
import com.joinhocus.horus.misc.Environment;
import com.joinhocus.horus.slack.SlackClient;
import com.joinhocus.horus.slack.SlackEventsHandler;
import com.joinhocus.horus.slack.horris.BasicHorrisExtension;
import com.joinhocus.horus.slack.waitlist.SlackWaitListExtension;
import io.javalin.Javalin;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.RequestLogger;
import io.javalin.plugin.json.JavalinJson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Horus {

    private final Logger logger;

    protected Horus() {
        try {
            logger = LoggerFactory.getLogger(Horus.class);
            logger.info(
                    "\n" +
                    " _   _                      \n" +
                    "| | | |                     \n" +
                    "| |_| | ___  _ __ _   _ ___ \n" +
                    "|  _  |/ _ \\| '__| | | / __|\n" +
                    "| | | | (_) | |  | |_| \\__ \\\n" +
                    "\\_| |_/\\___/|_|   \\__,_|___/\n"
            );
            //noinspection ResultOfMethodCallIgnored
            MongoDatabase.getInstance();
            //noinspection ResultOfMethodCallIgnored
            Environment.isDev();

            // create Javalin instance
            Javalin javalin = Javalin.create(config -> {
                config.showJavalinBanner = false;
                config.defaultContentType = "application/json";
                config.enableCorsForAllOrigins();
                config.requestLogger(new RequestLogger() {
                    @Override
                    public void handle(@NotNull Context context, @NotNull Float aFloat) throws Exception {
                        logger.info("Handling " + context.method() + " for " + context.path());
                    }
                });
            }).events(events -> {
                events.handlerAdded((info) -> {
                    if (info.getHandler() instanceof DefinedTypesHandler) {
                        //noinspection rawtypes
                        DefinedTypesHandler typedHandler = (DefinedTypesHandler) info.getHandler();
                        if (typedHandler.requestClass() == null) {
                            logger.warn("Tried to register handler with undefined request class {}", typedHandler.getClass().getCanonicalName());
                            return;
                        }
                    }
                    logger.info("Registered [" + info.getHttpMethod() + "] for " + info.getPath() + " (" + info.getHandler().getClass().getCanonicalName() + ")");
                });
            });
            // explicitly disable null serialization
            Gson JAVALIN_GSON = new GsonBuilder().create();
            JavalinJson.setFromJsonMapper(JAVALIN_GSON::fromJson);
            //noinspection NullableProblems
            JavalinJson.setToJsonMapper(JAVALIN_GSON::toJson);
            JavalinValidation.register(JsonObject.class, s -> JAVALIN_GSON.fromJson(s, JsonObject.class));
            registerRoutes(javalin);
            javalin.start();

            registerSlackExtensions();
            cleanupLogging(); // disable Apache weird logging from Unirest, we just want errors.
        } catch (Exception e) {
            throw new RuntimeException(e); // propagate
        }
    }

    private void registerRoutes(Javalin javalin) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        logger.info("Registering routes...");
        // account routes
        javalin.addHandler(HandlerType.POST, "/account/create", new AccountCreateHandler());
        javalin.addHandler(HandlerType.POST, "/account/login", new AccountLoginHandler());
        javalin.addHandler(HandlerType.POST, "/account/request_reset", new RequestPasswordResetHandler());
        javalin.addHandler(HandlerType.POST, "/account/reset_password", new ResetPasswordHandler());
        javalin.addHandler(HandlerType.POST, "/account/twitter/validate", new ValidateInviteTwitterComboHandler());
        javalin.addHandler(HandlerType.GET, "/account", new GetAccountHandler());
        javalin.addHandler(HandlerType.GET, "/account/inviter/info", new GetUserInviterInfo());
        javalin.addHandler(HandlerType.POST, "/account/code/validate", new ValidateInviteCodeHandler());
        javalin.addHandler(HandlerType.GET, "/account/checkUsername", new CheckAccountUsernameAvailabilityHandler());
        javalin.addHandler(HandlerType.POST, "/account/setup", new SetupAccountHandler());
        javalin.addHandler(HandlerType.PUT, "/account/settings/avatar", new UpdateAccountAvatarHandler());
        javalin.addHandler(HandlerType.GET, "/account/activity", new GetActivityFeedHandler());
        javalin.addHandler(HandlerType.GET, "/account/activity_count", new GetActivityCountHandler());
        javalin.addHandler(HandlerType.GET, "/account/invites", new GetCreatedInvitesHandler());
        javalin.addHandler(HandlerType.GET, "/account/organizations", new GetUserOrganizationsHandler());
        javalin.addHandler(HandlerType.PATCH, "/account/settings/change_password", new ChangePasswordHandler());

        // invites routes
        javalin.addHandler(HandlerType.POST, "/invite", new CreateInviteHandler());
        javalin.addHandler(HandlerType.GET, "/invite/validate/:inviteId", new ValidateInviteHandler());

        // waitlist
        javalin.addHandler(HandlerType.POST, "/waitlist/twitter/token", new JoinWaitlistTwitterHandler());
        javalin.addHandler(HandlerType.POST, "/waitlist/twitter/verify", new VerifyWaitlistTwitterHandler());
        javalin.addHandler(HandlerType.POST, "/waitlist/twitter/validate", new TwitterValidateWaitListHandler());
        javalin.addHandler(HandlerType.POST, "/waitlist/email/verify", new SendVerificationEmailHandler());
        javalin.addHandler(HandlerType.POST, "/waitlist/email/validate", new EmailValidateWaitlistHandler());
        javalin.addHandler(HandlerType.POST, "/register/twitter/token", new RegisterWithTwitterHandler());

        // organization
        javalin.addHandler(HandlerType.POST, "/organization/create", new CreateOrganizationHandler());
        javalin.addHandler(HandlerType.PUT, "/organization/settings/logo", new UpdateOrganizationLogoHandler());

        // post
        javalin.addHandler(HandlerType.POST, "/posts", new CreatePostHandler());
        javalin.addHandler(HandlerType.GET, "/feed", new GetFeedHandler());
        javalin.addHandler(HandlerType.POST, "/post/like", new LikePostHandler());
        javalin.addHandler(HandlerType.GET, "/posts", new GetPostByIdHandler());
        javalin.addHandler(HandlerType.POST, "/posts/reply", new ReplyToPostHandler());

        // twitter routes
        javalin.addHandler(HandlerType.GET, "/twitter/info/:twitterId", new TwitterInfoHandler());

        // misc
        javalin.addHandler(HandlerType.POST, "/entity/follow", new FollowEntityHandler());
        javalin.addHandler(HandlerType.GET, "/handles/search", new SearchByHandleHandler());
        javalin.addHandler(HandlerType.GET, "/entity/profile", new GetEntityProfileHandler());
        javalin.addHandler(HandlerType.GET, "/entity/follows", new GetFollowsEntityHandler());

        // internal stuff
        javalin.addHandler(HandlerType.POST, "/internal/slack/events", new SlackEventsHandler());

        logger.info("Done registering routes! {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    private void cleanupLogging() {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }

    private void registerSlackExtensions() {
        SlackClient client = SlackClient.getInstance();
        client.registerExtension(new SlackWaitListExtension());
        client.registerExtension(new BasicHorrisExtension());
    }

}
