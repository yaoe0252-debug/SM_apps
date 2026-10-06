package com.smapps.chat.protocol.envelope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.protocol.auth.LoginRequest;
import com.smapps.chat.protocol.type.MessageType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifies replacement of untrusted client time at the server boundary. */
class EnvelopeTest {

  @Test
  void shouldUseServerTimeWhenRequestIsReceived() {
    Instant clientTime = Instant.parse("2026-10-05T08:00:00Z");
    Instant serverTime = Instant.parse("2026-10-05T08:00:03Z");
    Envelope<LoginRequest> envelope =
        new Envelope<>(
            ProtocolVersion.CURRENT,
            MessageId.parse("10000000-0000-0000-0000-000000000001"),
            MessageType.LOGIN_REQUEST,
            null,
            clientTime,
            new LoginRequest("alice_demo", "Example-Only-Password"),
            null);

    Envelope<LoginRequest> received =
        envelope.withServerTimestamp(Clock.fixed(serverTime, ZoneOffset.UTC));

    assertEquals(serverTime, received.timestamp());
    assertEquals(envelope.messageId(), received.messageId());
  }
}
