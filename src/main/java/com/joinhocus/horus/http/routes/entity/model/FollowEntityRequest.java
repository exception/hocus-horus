package com.joinhocus.horus.http.routes.entity.model;

import com.joinhocus.horus.misc.follow.EntityType;
import lombok.Data;

@Data
public class FollowEntityRequest {
    private final String id;
    private final EntityType type;
}
