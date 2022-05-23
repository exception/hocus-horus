package com.joinhocus.horus.twitter.data;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TwitterUser {

    private final String name, location, url;
    @SerializedName("screen_name")
    private final String handle;
    @SerializedName("id_str")
    private final String id;
    private final boolean verified;
    @SerializedName("followers_count")
    private final int followers;

}
