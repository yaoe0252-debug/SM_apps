package com.smapps.chat.common.id;

import java.util.Objects;
import java.util.UUID;

/** Globally unique logical message identifier reused by safe retries. */
public record MessageId(UUID value) {

  public MessageId {
    Objects.requireNonNull(value, "value must not be null");
  }

  /** Parses the canonical UUID form used on the wire. */
  public static MessageId parse(String value) {
    return new MessageId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
  }

  /** Creates an identifier for a new logical request or server envelope. */
  public static MessageId random() {
    return new MessageId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
