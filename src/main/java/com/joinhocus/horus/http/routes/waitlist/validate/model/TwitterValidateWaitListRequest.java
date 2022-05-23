package com.joinhocus.horus.http.routes.waitlist.validate.model;

import lombok.Data;

@Data
public class TwitterValidateWaitListRequest {

    private final String twitterId, verify, token;

}
