package com.joinhocus.horus.http.routes.waitlist.verify.model;

import lombok.Data;

@Data
public class VerifyTwitterOAuthRequest {

    private final String token, verify;

}
