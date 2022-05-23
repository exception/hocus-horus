package com.joinhocus.horus.slack;

import com.slack.api.bolt.App;

public interface SlackClientExtension {

    void register(App app);

}
