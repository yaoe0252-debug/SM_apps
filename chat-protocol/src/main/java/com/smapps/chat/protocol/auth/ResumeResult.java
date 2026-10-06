package com.smapps.chat.protocol.auth;

import com.smapps.chat.protocol.chat.ChatEvent;
import com.smapps.chat.protocol.validation.ProtocolValidator;
import java.util.List;

/** Returns one bounded page of missed messages. */
public record ResumeResult(List<ChatEvent> missingMessages, String nextCursor, boolean hasMore) {

  public ResumeResult {
    ProtocolValidator.standard().validateResumeResult(missingMessages, nextCursor);
    missingMessages = List.copyOf(missingMessages);
  }
}
