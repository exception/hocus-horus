package com.joinhocus.horus.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Pair<Key, Value> {

    private final Key key;
    private final Value value;

    public static <Key, Value> Pair<Key, Value> of(Key key, Value value) {
        return new Pair<>(key, value);
    }

}
