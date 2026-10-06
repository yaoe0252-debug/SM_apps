package com.smapps.chat.protocol.auth;

import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.validation.ProtocolValidator;
import java.time.Instant;

/** Returns the new session token only through the successful login response. */
public record LoginResult(String sessionToken, Instant expiresAt, UserId userId) {

  public LoginResult {
    ProtocolValidator.standard().validateLoginResult(sessionToken, expiresAt, userId);
  }

  /** Keeps bearer tokens out of accidental diagnostic output. */
  @Override
  public String toString() {
    return "LoginResult[sessionToken=<redacted>, expiresAt="
        + expiresAt
        + ", userId="
        + userId
        + "]";
  }
}
