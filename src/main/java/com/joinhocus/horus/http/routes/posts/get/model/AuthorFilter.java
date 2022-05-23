package com.joinhocus.horus.http.routes.posts.get.model;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class AuthorFilter {
    private final ObjectId author;
    private final ObjectId organization;
}
