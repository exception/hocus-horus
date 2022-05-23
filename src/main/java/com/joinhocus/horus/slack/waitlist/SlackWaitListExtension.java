package com.joinhocus.horus.slack.waitlist;

import com.joinhocus.horus.slack.SlackClientExtension;
import com.slack.api.bolt.App;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.view.Views;

public class SlackWaitListExtension implements SlackClientExtension {
    @Override
    public void register(App app) {
        app.blockAction("give_access", (request, context) -> {
            context.respond("Not implemented yet!");
            return context.ack();
        });

        app.command("/waitlist", (request, context) -> {
            ViewsOpenResponse viewsOpen = context.client().viewsOpen(r -> r.triggerId(context.getTriggerId()).view(
                    Views.view(view -> view
                            .callbackId("invite_direct")
                            .type("modal")
                            .notifyOnClose(true)
                            .title(Views.viewTitle(title -> title.type("plain_text").text("Invite a Twitter user to Hocus")))
                            .submit(Views.viewSubmit(submit -> submit.type("plain_text").text("Invite!")))
                            .close(Views.viewClose(close -> close.type("plain_text").text("Cancel")))
                            .blocks(Blocks.asBlocks(
                                    Blocks.input(input -> {
                                        input.blockId("handle-block")
                                                .element(BlockElements.plainTextInput(pti -> pti.actionId("handle-action")))
                                                .label(BlockCompositions.plainText("Twitter Handle"));
                                        return input;
                                    })
                            )))
            ));
            if (viewsOpen.isOk()) return context.ack();
            return Response.builder().statusCode(500).body(viewsOpen.getError()).build();
        });
    }
}
