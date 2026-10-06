package com.smapps.chat.protocol.auth;

import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Revokes the supplied authenticated session. */
public record LogoutRequest(String sessionToken) {

  public LogoutRequest {
    ProtocolValidator.standard().validateSessionToken(sessionToken);
  }

  @Override
  public String toString() {
    return "LogoutRequest[sessionToken=<redacted>]";
  }
}
