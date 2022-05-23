package com.joinhocus.horus.account.activity;

import com.joinhocus.horus.misc.follow.EntityType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Getter
@ToString
public class ActivityNotification {

    private final ObjectId id;
    private final ObjectId actor;
    private final EntityType entityType;
    private final List<ObjectId> receivers;
    private final Date when;
    private final NotificationType notificationType;
    private final Document extra;

    private boolean wasSeen;

    public ActivityNotification(Document document) {
        this.id = document.getObjectId("_id");
        this.actor = document.getObjectId("actor");
        this.entityType = EntityType.valueOf(document.getString("entity"));
        //noinspection unchecked
        this.receivers = (List<ObjectId>) document.get("receivers");
        this.when = document.getDate("when");
        this.notificationType = NotificationType.valueOf(document.getString("type"));
        this.extra = document.get("extra", Document.class);
        this.wasSeen = document.getBoolean("wasSeen", false);
    }

}
