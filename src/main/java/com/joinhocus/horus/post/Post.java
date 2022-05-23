package com.joinhocus.horus.post;

import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.organization.Organization;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

public interface Post {

    ObjectId getId();

    PostType type();

    PostPrivacy privacy();

    String content();

    UserAccount author();

    Organization organization();

    Date postTime();

    ObjectId parent();

    void addComment(Post comment);

    List<Post> getComments();
}
