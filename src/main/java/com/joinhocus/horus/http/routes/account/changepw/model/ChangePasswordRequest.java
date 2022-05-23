package com.joinhocus.horus.http.routes.account.changepw.model;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    private final String current, changeTo;

}
