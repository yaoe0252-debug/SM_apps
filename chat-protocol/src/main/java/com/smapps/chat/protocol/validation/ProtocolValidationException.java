package com.smapps.chat.protocol.validation;

import com.smapps.chat.protocol.type.ErrorCode;
import java.util.Objects;

/** Validation failure carrying a stable client-safe error code. */
public final class ProtocolValidationException extends IllegalArgumentException {

  private final ErrorCode errorCode;

  public ProtocolValidationException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
