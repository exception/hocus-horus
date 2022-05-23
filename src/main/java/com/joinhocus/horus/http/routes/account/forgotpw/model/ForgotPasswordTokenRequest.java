package com.joinhocus.horus.http.routes.account.forgotpw.model;

import lombok.Data;

@Data
public class ForgotPasswordTokenRequest {

    private final String token;
    private final String password;

}
