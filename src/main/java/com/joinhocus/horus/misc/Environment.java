package com.joinhocus.horus.misc;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
@Getter
public enum Environment {
    PRODUCTION("https://joinhocus.com"),
    REVIEW("https://review.feed.horris.dev"), // TODO change this when working on branches w/ frontend
    STAGING("https://staging.horris.dev"),
    DEVELOPMENT("http://localhost:3000");

    private final String url;
    private static final Environment current;

    static {
        Logger logger = LoggerFactory.getLogger(Environment.class);
        String env = System.getenv("ENVIRONMENT");
        if (Strings.isNullOrEmpty(env)) {
            if (!Strings.isNullOrEmpty(System.getenv("GIT_HASH"))) {
                current = PRODUCTION;
            } else {
                current = DEVELOPMENT;
            }
        } else {
            current = Environment.valueOf(env);
        }

        logger.info("Loaded environment {}", current.name());
    }

    public static boolean isDev() {
        return current == DEVELOPMENT || current == STAGING;
    }

    public static Environment current() {
        return current;
    }
}
