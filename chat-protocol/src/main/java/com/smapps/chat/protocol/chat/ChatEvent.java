package com.smapps.chat.protocol.chat;

import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Server event delivered in conversation sequence order. */
public record ChatEvent(String conversationId, UserId senderId, String text, long serverSequence) {

  public ChatEvent {
    ProtocolValidator.standard().validateChatEvent(conversationId, senderId, text, serverSequence);
  }
}
