package com.smapps.chat.protocol.chat;

import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Sends text to an authorized topic subscription. */
public record ChatTopicSend(String topicId, String text) {

  public ChatTopicSend {
    ProtocolValidator.standard().validateTopicChat(topicId, text);
  }
}
