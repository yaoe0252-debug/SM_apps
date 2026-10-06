package com.smapps.chat.protocol.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.auth.LoginRequest;
import com.smapps.chat.protocol.chat.ChatPublicSend;
import com.smapps.chat.protocol.envelope.Envelope;
import com.smapps.chat.protocol.envelope.ProtocolVersion;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.MessageType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Exercises the shared validator at untrusted protocol boundaries. */
class ProtocolValidatorTest {

  private static final UserId ALICE = UserId.parse("20000000-0000-0000-0000-000000000001");
  private static final UserId BOB = UserId.parse("20000000-0000-0000-0000-000000000002");
  private static final Instant CLIENT_TIME = Instant.parse("2026-10-05T08:00:00Z");

  @Test
  void shouldAcceptMatchingSenderWhenSessionIsAuthenticated() {
    Envelope<ChatPublicSend> envelope =
        envelope(MessageType.CHAT_PUBLIC_SEND, ALICE, new ChatPublicSend("hello"));

    assertDoesNotThrow(() -> ProtocolValidator.standard().validateClientEnvelope(envelope, ALICE));
  }

  @Test
  void shouldRejectSenderWhenSessionIdentityDiffers() {
    Envelope<ChatPublicSend> envelope =
        envelope(MessageType.CHAT_PUBLIC_SEND, ALICE, new ChatPublicSend("hello"));

    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () -> ProtocolValidator.standard().validateClientEnvelope(envelope, BOB));

    assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
  }

  @Test
  void shouldRejectSenderWhenAnonymousRequestDeclaresIdentity() {
    Envelope<LoginRequest> envelope =
        envelope(
            MessageType.LOGIN_REQUEST,
            ALICE,
            new LoginRequest("alice_demo", "Example-Only-Password"));

    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () -> ProtocolValidator.standard().validateClientEnvelope(envelope, null));

    assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
  }

  @Test
  void shouldRejectPayloadWhenMessageTypeDoesNotMatch() {
    Envelope<LoginRequest> envelope =
        envelope(
            MessageType.CHAT_PUBLIC_SEND,
            ALICE,
            new LoginRequest("alice_demo", "Example-Only-Password"));

    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () -> ProtocolValidator.standard().validateClientEnvelope(envelope, ALICE));

    assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode());
  }

  @Test
  void shouldRejectTextWhenOnlyWhitespaceIsProvided() {
    assertThrows(ProtocolValidationException.class, () -> new ChatPublicSend("   "));
  }

  @Test
  void shouldRejectFrameWhenByteLimitIsExceeded() {
    ProtocolValidationException exception =
        assertThrows(
            ProtocolValidationException.class,
            () -> ProtocolValidator.standard().validateFrameSize(64 * 1024 + 1));

    assertEquals(ErrorCode.LIMIT_EXCEEDED, exception.errorCode());
  }

  private static <T> Envelope<T> envelope(MessageType messageType, UserId senderId, T payload) {
    return new Envelope<>(
        ProtocolVersion.CURRENT,
        MessageId.parse("10000000-0000-0000-0000-000000000001"),
        messageType,
        senderId,
        CLIENT_TIME,
        payload,
        null);
  }
}
