package com.smapps.chat.protocol.chat;

import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Sends text to one authorized recipient. */
public record ChatPrivateSend(UserId recipientId, String text) {

  public ChatPrivateSend {
    ProtocolValidator.standard().validatePrivateChat(recipientId, text);
  }
}
