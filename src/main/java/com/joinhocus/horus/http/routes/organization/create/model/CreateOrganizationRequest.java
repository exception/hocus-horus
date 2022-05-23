package com.joinhocus.horus.http.routes.organization.create.model;

import lombok.Data;

@Data
public class CreateOrganizationRequest {
    private final String name, handle;
}
