package com.joinhocus.horus.slack;

import com.google.common.base.Joiner;
import com.slack.api.bolt.request.Request;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.InternalServerErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SlackEventsHandler implements Handler {

    @Override
    public void handle(@NotNull Context context) {
        Logger logger = LoggerFactory.getLogger(getClass());
        Request<?> slackReq = SlackClient.getInstance().transform(context);
        if (slackReq != null) {
            com.slack.api.bolt.context.Context slackContext = slackReq.getContext();
            slackContext.setBotToken(SlackClient.XOXB_TOKEN);
            try {
                com.slack.api.bolt.response.Response slackResponse = SlackClient.getInstance().getApp().run(slackReq);
                if (slackResponse != null) {
                    context.status(slackResponse.getStatusCode());
                    for (Map.Entry<String, List<String>> header : slackResponse.getHeaders().entrySet()) {
                        String name = header.getKey();
                        context.header(name, Joiner.on(",").join(header.getValue()));
                    }
                    if (slackResponse.getBody() != null) {
                        context.result(slackResponse.getBody());
                    } else {
                        context.status(404); // not found
                        // if it's null we let it time out, slack handles that for us
                    }
                }
            } catch (Exception e) {
                logger.error("", e);
                context.json(new InternalServerErrorResponse());
            }
        }
    }

}
