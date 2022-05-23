package com.joinhocus.horus.account.invite;

import com.google.common.base.Strings;
import lombok.Getter;
import org.bson.Document;
import org.bson.types.ObjectId;

@Getter
public class Invite {

    // either of these two MUST be present
    private String inviteId;

    private final String twitterId, email;
    private final ObjectId inviter; // if this is null = horris invited.
    private final String orgId;
    private final String code;

    private boolean wasClaimed;

    public Invite(Document document) {
        this.inviteId = document.getObjectId("_id").toHexString();
        this.twitterId = document.getString("twitterId");
        this.email = document.getString("email");
        this.inviter = document.getObjectId("inviter");
        this.orgId = document.getString("orgId");
        this.code = document.getString("code");
        this.wasClaimed = document.getBoolean("claimed", false);
    }

    public Invite(String twitterId, String email, ObjectId inviter, String orgId, String code) {
        if (Strings.isNullOrEmpty(twitterId) && Strings.isNullOrEmpty(email)) {
            throw new IllegalStateException("twitterId or email must be provided");
        }
        this.twitterId = twitterId;
        this.email = email;
        this.inviter = inviter;
        this.orgId = orgId;
        this.code = code;
    }

    public Document toDocument() {
        return new Document()
                .append("twitterId", this.twitterId)
                .append("email", this.email)
                .append("inviter", this.inviter)
                .append("orgId", this.orgId)
                .append("code", this.code)
                .append("generatedAt", System.currentTimeMillis());
    }
}
