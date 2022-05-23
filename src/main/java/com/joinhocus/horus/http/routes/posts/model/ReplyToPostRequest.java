package com.joinhocus.horus.http.routes.posts.model;

import lombok.Data;

@Data
public class ReplyToPostRequest {

    private final String content;
    private final String parent;

}
