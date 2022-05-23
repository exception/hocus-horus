package com.joinhocus.horus.http.routes.waitlist;

import com.joinhocus.horus.misc.Environment;
import com.joinhocus.horus.slack.SlackClient;
import com.joinhocus.horus.twitter.TwitterAPI;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.composition.ConfirmationDialogObject;
import com.slack.api.model.block.element.BlockElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.concurrent.ForkJoinPool;

public class WaitlistUtil {

    public static void sendToSlack(String userId, String inviteId) {
        if (Environment.isDev()) return; // don't want to do this for dev env
        Logger logger = LoggerFactory.getLogger(WaitlistUtil.class);
        ForkJoinPool.commonPool().submit(() -> {
            TwitterAPI.fetchUserInfo(userId).thenAccept(user -> {
                try {
                    SlackClient.getInstance().getClient().chatPostMessage(
                            ChatPostMessageRequest.builder()
                                    .channel("#waitlist")
                                    .blocks(Blocks.asBlocks(
                                            Blocks.header(section -> section.text(BlockCompositions.plainText(":tada: New wait-list sign-up!"))),
                                            Blocks.section(section -> section.fields(
                                                    BlockCompositions.asSectionFields(
                                                            BlockCompositions.markdownText("*Handle*:\n<https://twitter.com/" + user.getHandle() + "|" + user.getName() + ">"),
                                                            BlockCompositions.markdownText("*Verified*:\n" + (user.isVerified() ? "Yes" : "No")),
                                                            BlockCompositions.markdownText("*Location*:\n" + user.getLocation()),
                                                            BlockCompositions.markdownText("*Followers*:\n" + NumberFormat.getInstance().format(user.getFollowers()))
                                                    )
                                            )),
                                            Blocks.divider(),
                                            Blocks.actions(
                                                    BlockElements.asElements(
                                                            BlockElements.button(button -> {
                                                                button.actionId("give_access");
                                                                button.text(BlockCompositions.plainText("Give Early Access"));
                                                                button.value(inviteId);
                                                                button.style("primary");
                                                                button.confirm(ConfirmationDialogObject.builder()
                                                                        .title(BlockCompositions.plainText("Give " + user.getName() + " early access?"))
                                                                        .text(BlockCompositions.plainText("Please confirm if you wish to give " + user.getName() + " early access to Hocus"))
                                                                        .confirm(BlockCompositions.plainText("Yes"))
                                                                        .deny(BlockCompositions.plainText("No"))
                                                                        .build()
                                                                );
                                                                return button;
                                                            })
                                                    )
                                            )
                                    ))
                                    .build()
                    );
                } catch (Exception e) {
                    logger.error("", e);
                }
            }).exceptionally(err -> {
                logger.error("", err);
                return null;
            });
        });
    }

}
