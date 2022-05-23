package com.joinhocus.horus.http.routes.account.login.model;

import lombok.Data;

@Data
public class AccountLoginRequest {

    private final String login, password;

}
