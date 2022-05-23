package com.joinhocus.horus.http.routes.account.create.model;

import lombok.Data;

@Data
public class VerifyComboRequest {

    private final String inviteCode, twitterId;

}
