package com.joinhocus.horus.organization;

import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.misc.follow.Followable;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Organization extends Followable {

    ObjectId getId();

    String getName();

    String getHandle();

    OrganizationSettings getSettings();

    Map<ObjectId, OrganizationRole> getSeats();

    CompletableFuture<Map<UserAccount, OrganizationRole>> getUserSeats();

//    default CompletableFuture<Organization> getFromDocument(Document document) {
//        ObjectId orgId = document.getObjectId("_id");
//        Document nameDoc = document.get("name", Document.class);
//        String name = nameDoc.getString("display");
//        String handle = nameDoc.getString("handle");
//        OrganizationSettings settings;
//        if (document.containsKey("settings")) {
//            settings = new OrganizationSettings(document.get("settings", Document.class));
//        } else {
//            settings = new OrganizationSettings();
//        }
//
//        if (document.containsKey("seats")) {
//            //noinspection unchecked
//            List<Document> members = (List<Document>) document.get("seats");
//
//            Map<ObjectId, OrganizationRole> roleMap = new HashMap<>();
//            Map<ObjectId, CompletableFuture<UserAccount>> seatMap = new HashMap<>();
//            for (Document member : members) {
//                ObjectId id = member.getObjectId("id");
//                OrganizationRole role = OrganizationRole.valueOf(member.getString("role"));
//
//                roleMap.put(id, role);
//                seatMap.put(id, MongoDatabase.getInstance().getRepo(AccountsRepo.class).findById(id));
//            }
//
//            return CompletableFutures.asMap(seatMap).thenApply(outMap -> {
//                Map<UserAccount, OrganizationRole> seats = new HashMap<>(outMap.size());
//                outMap.forEach((id, account) -> {
//                    seats.put(account, roleMap.get(id));
//                });
//
//                return new SimpleOrganization(
//                        name,
//                        handle,
//                        settings,
//                        orgId,
//                        seats
//                );
//            });
//        }
//
//        return CompletableFuture.completedFuture(new SimpleOrganization(
//                name,
//                handle,
//                settings,
//                orgId,
//                Collections.emptyMap()
//        ));
//    }

}
