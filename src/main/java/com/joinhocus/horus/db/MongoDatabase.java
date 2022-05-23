package com.joinhocus.horus.db;

import com.joinhocus.horus.config.Configs;
import com.joinhocus.horus.db.config.MongoConfig;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.db.repos.ActivityRepo;
import com.joinhocus.horus.db.repos.EmailInviteVerificationRepo;
import com.joinhocus.horus.db.repos.FollowsRepo;
import com.joinhocus.horus.db.repos.InvitesRepo;
import com.joinhocus.horus.db.repos.LikesRepo;
import com.joinhocus.horus.db.repos.OrganizationRepo;
import com.joinhocus.horus.db.repos.PasswordResetRepo;
import com.joinhocus.horus.db.repos.PostsRepo;
import com.joinhocus.horus.db.repos.WaitListRepo;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.selector.ReadPreferenceServerSelector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MongoDatabase {

    private final Map<Class<? extends AsyncMongoRepo>, AsyncMongoRepo> moduleMap = new HashMap<>();
    private MongoClient client;

    private static final MongoDatabase instance;

    static {
        instance = new MongoDatabase();
    }

    MongoDatabase() {
        try {
            registerDatabase();
            registerModules();
        } catch (Throwable t) {
            throw new RuntimeException(t); // propagate.
        }
    }

    private void registerDatabase() {
        MongoConfig config = Configs.load(MongoConfig.class,
                mongoConfigClass -> new MongoConfig("root", "", Collections.emptyList())
        );
        // auth on admin database, that way we get access to all the rest
        MongoCredential credential = MongoCredential.createCredential(config.getUsername(), "admin", config.getPassword().toCharArray());
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyToClusterSettings(b -> {
                    b.hosts(config.asServerAddresses());
                    b.mode(config.getAddresses().size() > 1 ? ClusterConnectionMode.MULTIPLE : ClusterConnectionMode.SINGLE);
                    b.serverSelector(new ReadPreferenceServerSelector(ReadPreference.primary()));
                }).applyToConnectionPoolSettings(b -> {
                    b.maxWaitTime(1, TimeUnit.SECONDS);
                    b.maxConnectionIdleTime(5, TimeUnit.SECONDS);
                });

        if (!config.getUsername().isEmpty()) {
            builder.credential(credential);
        }

        this.client = MongoClients.create(builder.build());
    }

    private void registerModules() {
        registerRepo(new AccountsRepo());
        registerRepo(new PasswordResetRepo());
        registerRepo(new InvitesRepo());
        registerRepo(new WaitListRepo());
        registerRepo(new EmailInviteVerificationRepo());
        registerRepo(new OrganizationRepo());
        registerRepo(new PostsRepo());
        registerRepo(new LikesRepo());
        registerRepo(new FollowsRepo());
        registerRepo(new ActivityRepo());
    }

    public static MongoDatabase getInstance() {
        return instance;
    }

    private void registerRepo(AsyncMongoRepo repo) {
        repo.setDatabase(this);
        this.moduleMap.put(repo.getClass(), repo);
        repo.postBoot();
    }

    public MongoClient getClient() {
        return client;
    }

    public <T extends AsyncMongoRepo> T getRepo(Class<T> clazz) {
        //noinspection unchecked
        return (T) moduleMap.get(clazz);
    }
}
