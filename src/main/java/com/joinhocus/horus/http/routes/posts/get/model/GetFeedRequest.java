package com.joinhocus.horus.http.routes.posts.get.model;

import com.joinhocus.horus.post.PostType;
import lombok.Data;

import java.util.Set;

@Data
public class GetFeedRequest {

    private final int page;
    private final Set<PostType> types;
    private final AuthorFilter authorFilter;

}
