package com.joinhocus.horus.post.impl;

import com.joinhocus.horus.account.UserAccount;
import com.joinhocus.horus.organization.Organization;
import com.joinhocus.horus.post.Post;
import com.joinhocus.horus.post.PostPrivacy;
import com.joinhocus.horus.post.PostType;
import lombok.ToString;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

@ToString
public class BasicPost implements Post {

    private final ObjectId id;
    private final PostType type;
    private final PostPrivacy privacy;
    private final String content;
    private final UserAccount author;
    private final Organization organization;
    private final Date postTime;
    private final ObjectId parent;
    private final List<Post> comments;

    public BasicPost(
            ObjectId id,
            PostType type,
            PostPrivacy privacy,
            String content,
            UserAccount author,
            Organization organization,
            Date postTime,
            ObjectId parent,
            List<Post> comments
    ) {
        this.id = id;
        this.type = type;
        this.privacy = privacy;
        this.content = content;
        this.comments = comments;
        this.author = author;
        this.organization = organization;
        this.postTime = postTime;
        this.parent = parent;
    }

    @Override
    public ObjectId getId() {
        return this.id;
    }

    @Override
    public PostType type() {
        return this.type;
    }

    @Override
    public PostPrivacy privacy() {
        return this.privacy;
    }

    @Override
    public String content() {
        return this.content;
    }

    @Override
    public UserAccount author() {
        return this.author;
    }

    @Override
    public Organization organization() {
        return this.organization;
    }

    @Override
    public Date postTime() {
        return this.postTime;
    }

    @Override
    public ObjectId parent() {
        return this.parent;
    }

    @Override
    public List<Post> getComments() {
        return comments;
    }

    @Override
    public void addComment(Post comment) {
        this.comments.add(comment);
    }
}
