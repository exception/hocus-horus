package com.joinhocus.horus.organization;

import lombok.Getter;
import org.bson.Document;

@Getter
public class OrganizationSettings {

    private final String logo;

    public OrganizationSettings(Document document) {
        this.logo = document.getString("logo");
    }

    public OrganizationSettings() {
        this.logo = null;
    }

}
