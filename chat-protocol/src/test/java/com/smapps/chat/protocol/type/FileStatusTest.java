package com.smapps.chat.protocol.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smapps.chat.protocol.validation.ProtocolValidationException;
import org.junit.jupiter.api.Test;

/** Verifies that delayed file events cannot move a terminal state backwards. */
class FileStatusTest {

  @Test
  void shouldAllowForwardTransitionWhenUploadCompletes() {
    assertTrue(FileStatus.UPLOADING.canTransitionTo(FileStatus.AVAILABLE));
  }

  @Test
  void shouldRejectBackwardTransitionWhenFileIsAvailable() {
    assertFalse(FileStatus.AVAILABLE.canTransitionTo(FileStatus.UPLOADING));
  }

  @Test
  void shouldReturnStableStatusWhenWireValueIsKnown() {
    assertEquals(FileStatus.AVAILABLE, FileStatus.fromWireValue("AVAILABLE"));
  }

  @Test
  void shouldRejectStatusWhenWireValueIsUnknown() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class, () -> FileStatus.fromWireValue("QUARANTINED"));

    assertEquals(ErrorCode.UNSUPPORTED_MESSAGE_TYPE, exception.errorCode());
  }
}
