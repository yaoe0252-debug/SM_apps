package com.smapps.chat.protocol.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smapps.chat.protocol.validation.ProtocolValidationException;
import org.junit.jupiter.api.Test;

/** Verifies registration boundaries and diagnostic password redaction. */
class RegisterRequestTest {

  @Test
  void shouldHidePasswordWhenConvertedToString() {
    String password = "Example-Only-Password";

    String diagnosticText = new RegisterRequest("alice_demo", password).toString();

    assertFalse(diagnosticText.contains(password));
  }

  @Test
  void shouldRejectUsernameWhenUtf8ByteLimitIsExceeded() {
    String username = "用".repeat(32);

    assertThrows(
        ProtocolValidationException.class,
        () -> new RegisterRequest(username, "Example-Only-Password"));
  }
}
