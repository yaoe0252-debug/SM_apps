package com.smapps.chat.protocol.type;

import com.smapps.chat.protocol.validation.ProtocolValidationException;
import java.util.Arrays;

/** Stable error codes safe to expose to protocol clients. */
public enum ErrorCode {
  INVALID_INPUT("INVALID_INPUT"),
  UNSUPPORTED_VERSION("UNSUPPORTED_VERSION"),
  UNSUPPORTED_MESSAGE_TYPE("UNSUPPORTED_MESSAGE_TYPE"),
  UNAUTHENTICATED("UNAUTHENTICATED"),
  SESSION_EXPIRED("SESSION_EXPIRED"),
  FORBIDDEN("FORBIDDEN"),
  NOT_FOUND("NOT_FOUND"),
  USERNAME_TAKEN("USERNAME_TAKEN"),
  INVALID_CREDENTIALS("INVALID_CREDENTIALS"),
  LIMIT_EXCEEDED("LIMIT_EXCEEDED"),
  CONFLICT("CONFLICT"),
  RATE_LIMITED("RATE_LIMITED"),
  TEMPORARY_UNAVAILABLE("TEMPORARY_UNAVAILABLE"),
  INTERNAL_ERROR("INTERNAL_ERROR");

  private final String wireValue;

  ErrorCode(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  /** Rejects unknown external values so clients cannot invent error semantics. */
  public static ErrorCode fromWireValue(String value) {
    return Arrays.stream(values())
        .filter(code -> code.wireValue.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new ProtocolValidationException(
                    UNSUPPORTED_MESSAGE_TYPE, "Unknown error code: " + value));
  }
}
