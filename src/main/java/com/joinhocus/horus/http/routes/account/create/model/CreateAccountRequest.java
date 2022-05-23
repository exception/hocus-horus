package com.joinhocus.horus.http.routes.account.create.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateAccountRequest {

    private final String email, password, name, username;
    private final String inviteCode;

}
