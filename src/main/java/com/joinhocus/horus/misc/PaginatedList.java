package com.joinhocus.horus.misc;

import com.google.gson.JsonObject;
import lombok.Data;

import java.util.List;

@Data
public class PaginatedList<T> {
    private final JsonObject paginationData;
    private final List<T> data;
}
