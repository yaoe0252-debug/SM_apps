package com.smapps.chat.common.id;

import java.util.Objects;
import java.util.UUID;

/** Stable file transfer identifier that never exposes a storage path. */
public record FileId(UUID value) {

  public FileId {
    Objects.requireNonNull(value, "value must not be null");
  }

  /** Parses the canonical UUID form used on the wire. */
  public static FileId parse(String value) {
    return new FileId(UUID.fromString(Objects.requireNonNull(value, "value must not be null")));
  }

  /** Creates an identifier for a newly declared file transfer. */
  public static FileId random() {
    return new FileId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
