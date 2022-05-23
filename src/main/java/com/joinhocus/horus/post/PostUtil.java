package com.joinhocus.horus.post;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.organization.Organization;
import com.joinhocus.horus.post.impl.PostWithExtra;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PostUtil {

    public JsonObject recursive(Post post) {
        JsonObject parent = new JsonObject();
        JsonArray replies = new JsonArray();

        parent.addProperty("id", post.getId().toHexString());
        UserAccount authorAccount = post.author();
        JsonObject author = new JsonObject();
        if (authorAccount != null) {
            author.addProperty("id", authorAccount.getId().toHexString());
            author.addProperty("name", authorAccount.getName());
            author.addProperty("handle", authorAccount.getUsernames().getDisplay());
        }

        JsonObject organization = new JsonObject();
        Organization organizationObject = post.organization();
        if (organizationObject != null) {
            organization.addProperty("id", organizationObject.getId().toHexString());
            organization.addProperty("name", organizationObject.getName());
            organization.addProperty("logo", organizationObject.getSettings().getLogo());
        }

        parent.addProperty("content", post.content());
        if (post.type() != null) {
            parent.addProperty("type", post.type().name());
        }
        parent.addProperty("postTime", post.postTime().getTime());

        if (post instanceof PostWithExtra) {
            PostWithExtra extra = (PostWithExtra) post;
            JsonObject extraData = extra.getExtra();
            parent.add("extra", extraData);
        }

        for (Post comment : post.getComments()) {
            replies.add(recursive(comment));
        }

        parent.add("organization", organization);
        parent.add("author", author);
        if (replies.size() > 0) {
            parent.add("replies", replies);
        }
        return parent;
    }

}
