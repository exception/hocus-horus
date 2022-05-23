package com.joinhocus.horus.misc;

import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class MongoIds {

    @Nullable
    public ObjectId parseId(String id) {
        try {
            return new ObjectId(id);
        } catch (Exception e) {
            return null;
        }
    }

}
