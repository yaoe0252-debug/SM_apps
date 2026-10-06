package com.smapps.chat.protocol.auth;

import com.smapps.chat.common.id.UserId;
import java.util.Objects;

/** Confirms successful registration without returning password material. */
public record RegisterResult(UserId userId) {

  public RegisterResult {
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
