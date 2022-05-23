package com.joinhocus.horus.slack.horris;

import com.joinhocus.horus.slack.SlackClientExtension;
import com.slack.api.bolt.App;

public class BasicHorrisExtension implements SlackClientExtension {
    @Override
    public void register(App app) {
        app.command("/horris", (request, context) -> {
            return context.ack("Hocus pocus, it's time to focus! :sparkles:");
        });
    }
}
