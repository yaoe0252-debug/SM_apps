package com.smapps.chat.protocol.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smapps.chat.protocol.validation.ProtocolValidationException;
import org.junit.jupiter.api.Test;

/** Verifies stable message names and explicit rejection of unknown values. */
class MessageTypeTest {

  @Test
  void shouldReturnStableTypeWhenWireValueIsKnown() {
    assertEquals(MessageType.CHAT_EVENT, MessageType.fromWireValue("CHAT_EVENT"));
  }

  @Test
  void shouldRejectValueWhenWireValueIsUnknown() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class, () -> MessageType.fromWireValue("FUTURE_MESSAGE"));

    assertEquals(ErrorCode.UNSUPPORTED_MESSAGE_TYPE, exception.errorCode());
  }
}
