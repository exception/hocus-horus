package com.joinhocus.horus.post;

import java.util.EnumSet;

public enum PostType {
    UPDATE,
    WIN,
    ASK;

    public static EnumSet<PostType> ALL = EnumSet.allOf(PostType.class);
}
