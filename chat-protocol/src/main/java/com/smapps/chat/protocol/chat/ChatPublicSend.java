package com.smapps.chat.protocol.chat;

import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Sends text to the public conversation. */
public record ChatPublicSend(String text) {

  public ChatPublicSend {
    ProtocolValidator.standard().validateChatText(text);
  }
}
