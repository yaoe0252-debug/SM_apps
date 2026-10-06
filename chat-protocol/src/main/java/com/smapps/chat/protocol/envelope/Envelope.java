package com.smapps.chat.protocol.envelope;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.type.MessageType;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Versioned wire envelope shared by every request, response, and event. */
public record Envelope<T>(
    String protocolVersion,
    MessageId messageId,
    MessageType messageType,
    UserId senderId,
    Instant timestamp,
    T payload,
    MessageId requestId) {

  public Envelope {
    Objects.requireNonNull(protocolVersion, "protocolVersion must not be null");
    Objects.requireNonNull(messageId, "messageId must not be null");
    Objects.requireNonNull(messageType, "messageType must not be null");
    Objects.requireNonNull(timestamp, "timestamp must not be null");
    Objects.requireNonNull(payload, "payload must not be null");
  }

  /** Replaces the diagnostic client time with the authoritative server receive time. */
  public Envelope<T> withServerTimestamp(Clock clock) {
    Objects.requireNonNull(clock, "clock must not be null");
    return new Envelope<>(
        protocolVersion, messageId, messageType, senderId, clock.instant(), payload, requestId);
  }
}
