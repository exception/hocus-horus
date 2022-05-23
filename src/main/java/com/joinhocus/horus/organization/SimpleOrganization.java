package com.joinhocus.horus.organization;

import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.db.MongoDatabase;
import com.joinhocus.horus.db.repos.AccountsRepo;
import com.joinhocus.horus.misc.CompletableFutures;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ToString
@RequiredArgsConstructor
public class SimpleOrganization implements Organization {

    private final String name, handle;
    private final OrganizationSettings settings;
    private final ObjectId objectId;
    private final Map<ObjectId, OrganizationRole> seats;

    public SimpleOrganization(String name, String handle) {
        this.name = name;
        this.handle = handle;
        this.settings = new OrganizationSettings();
        this.objectId = null;
        this.seats = Collections.emptyMap();
    }

    public SimpleOrganization(Document document) {
        Document name = document.get("name", Document.class);
        this.objectId = document.getObjectId("_id");
        this.name = name.getString("display");
        this.handle = name.getString("handle");

        if (document.containsKey("settings")) {
            this.settings = new OrganizationSettings(document.get("settings", Document.class));
        } else {
            this.settings = new OrganizationSettings();
        }

        if (document.containsKey("seats")) {
            //noinspection unchecked
            List<Document> members = (List<Document>) document.get("seats");
            this.seats = new HashMap<>(members.size());
            for (Document member : members) {
                ObjectId id = member.getObjectId("id");
                OrganizationRole role = OrganizationRole.valueOf(member.getString("role"));

                this.seats.put(id, role);
            }
        } else {
            this.seats = new HashMap<>();
        }
    }

    @Override
    public ObjectId getId() {
        return this.objectId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getHandle() {
        return this.handle;
    }

    @Override
    public OrganizationSettings getSettings() {
        return this.settings;
    }

    @Override
    public Map<ObjectId, OrganizationRole> getSeats() {
        return this.seats;
    }

    @Override
    public CompletableFuture<Map<UserAccount, OrganizationRole>> getUserSeats() {
        Map<ObjectId, CompletableFuture<UserAccount>> futures = new HashMap<>();
        for (Map.Entry<ObjectId, OrganizationRole> member : this.seats.entrySet()) {
            futures.put(member.getKey(), MongoDatabase.getInstance().getRepo(AccountsRepo.class).findById(member.getKey()));
        }

        return CompletableFutures.asMap(futures).thenApply(outMap -> {
            Map<UserAccount, OrganizationRole> roles = new HashMap<>(outMap.size());
            outMap.forEach((id, account) -> {
                roles.put(account, seats.get(id));
            });

            return roles;
        });
    }
}
