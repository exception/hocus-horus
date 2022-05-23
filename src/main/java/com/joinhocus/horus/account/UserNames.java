package com.joinhocus.horus.account;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class UserNames {

    public static final Pattern NAME_PATTERN = Pattern.compile("^(\\w){1,15}$");

    private final String name, display;

}
