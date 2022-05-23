package com.joinhocus.horus.misc.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bson.types.ObjectId;

import java.lang.reflect.Type;

public class ObjectIdSerializer implements JsonSerializer<ObjectId>, JsonDeserializer<ObjectId> {

    @Override
    public ObjectId deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new ObjectId(jsonElement.getAsString());
    }

    @Override
    public JsonElement serialize(ObjectId id, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(id.toHexString());
    }
}
