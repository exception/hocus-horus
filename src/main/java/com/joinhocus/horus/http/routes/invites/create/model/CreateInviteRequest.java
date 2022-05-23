package com.joinhocus.horus.http.routes.invites.create.model;

import lombok.Data;

@Data
public class CreateInviteRequest {

    private final String sendTo;
    private final String orgId;
    private final boolean handleDeliver;

}
