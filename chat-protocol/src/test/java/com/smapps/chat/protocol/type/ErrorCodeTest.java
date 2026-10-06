package com.smapps.chat.protocol.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smapps.chat.protocol.validation.ProtocolValidationException;
import org.junit.jupiter.api.Test;

/** Verifies that externally visible error names remain explicit and stable. */
class ErrorCodeTest {

  @Test
  void shouldReturnStableCodeWhenWireValueIsKnown() {
    assertEquals(ErrorCode.FORBIDDEN, ErrorCode.fromWireValue("FORBIDDEN"));
  }

  @Test
  void shouldRejectCodeWhenWireValueIsUnknown() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class, () -> ErrorCode.fromWireValue("UNKNOWN_FAILURE"));

    assertEquals(ErrorCode.UNSUPPORTED_MESSAGE_TYPE, exception.errorCode());
  }
}
