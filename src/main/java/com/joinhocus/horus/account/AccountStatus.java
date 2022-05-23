package com.joinhocus.horus.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AccountStatus {
    AVAILABLE(""),
    USERNAME_TAKEN("an account with that username already exists"),
    EMAIL_TAKEN("an account with that email already exists"),
    UNKNOWN("unknown reason, please report this.");

    private final String message;
}
