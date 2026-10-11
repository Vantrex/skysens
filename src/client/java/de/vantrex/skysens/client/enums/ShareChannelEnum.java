package de.vantrex.skysens.client.enums;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Chat channels a message can be shared in. */
@Getter
@RequiredArgsConstructor
public enum ShareChannelEnum {

    PARTY("Party", List.of(ChatChannel.PARTY)),
    COOP("Co-op", List.of(ChatChannel.COOP)),
    BOTH("Party and Co-op", List.of(ChatChannel.PARTY, ChatChannel.COOP));

    private final String displayName;
    private final List<ChatChannel> channels;

    @Override
    public String toString() {
        return this.displayName;
    }

}
