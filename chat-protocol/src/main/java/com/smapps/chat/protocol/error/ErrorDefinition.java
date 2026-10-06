package com.smapps.chat.protocol.error;

import java.util.Objects;

/** Transport mapping and safe user guidance for one protocol error code. */
public record ErrorDefinition(
    int httpStatus, WssFailureAction wssAction, RetryPolicy retryPolicy, String userMessage) {

  public ErrorDefinition {
    if (httpStatus < 400 || httpStatus > 599) {
      throw new IllegalArgumentException("httpStatus must be between 400 and 599");
    }
    Objects.requireNonNull(wssAction, "wssAction must not be null");
    Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
    Objects.requireNonNull(userMessage, "userMessage must not be null");
    if (userMessage.isBlank()) {
      throw new IllegalArgumentException("userMessage must not be blank");
    }
  }
}
