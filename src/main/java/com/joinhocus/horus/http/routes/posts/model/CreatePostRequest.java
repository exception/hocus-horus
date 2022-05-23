package com.joinhocus.horus.http.routes.posts.model;

import com.joinhocus.horus.post.PostPrivacy;
import com.joinhocus.horus.post.PostType;
import lombok.Data;

@Data
public class CreatePostRequest {

    private final String content;
    private final PostType type;
    private final PostPrivacy privacy;
    private final String organizationId;

}
