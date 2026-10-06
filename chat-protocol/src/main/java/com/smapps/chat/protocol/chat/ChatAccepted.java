package com.smapps.chat.protocol.chat;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Confirms that a logical chat message has been durably accepted. */
public record ChatAccepted(MessageId messageId, long serverSequence) {

  public ChatAccepted {
    ProtocolValidator.standard().validateAcceptedMessage(messageId, serverSequence);
  }
}
