package com.joinhocus.horus.config;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.function.Function;

public class Configs {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static <T> T load(Class<T> clazz, Function<Class<T>, T> fallback) {
        Config config = clazz.getAnnotation(Config.class);
        Preconditions.checkNotNull(config, "Config not present");

        T object = null;
        File file = new File(
                "configs/" + config.directory(),
                config.name() + ".json"
        );
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file))) {
            object = GSON.fromJson(reader, clazz);
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                if (fallback != null) {
                    object = fallback.apply(clazz);
                }
            }

            e.printStackTrace();
        }

        return object;
    }
}
