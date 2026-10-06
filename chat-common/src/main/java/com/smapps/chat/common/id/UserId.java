package com.smapps.chat.common.id;

import java.util.Objects;
import java.util.UUID;

/** Stable user identifier shared by protocol and server boundaries. */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value must not be null");
  }

  /** Parses the canonical UUID form used on the wire. */
  public static UserId parse(String value) {
    return new UserId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
  }

  /** Creates an identifier for a newly registered user. */
  public static UserId random() {
    return new UserId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
