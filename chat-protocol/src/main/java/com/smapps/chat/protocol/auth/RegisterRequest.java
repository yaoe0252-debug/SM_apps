package com.smapps.chat.protocol.auth;

import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Requests creation of a user account. */
public record RegisterRequest(String username, String password) {

  public RegisterRequest {
    ProtocolValidator.standard().validateCredentials(username, password);
  }

  /** Keeps passwords out of accidental diagnostic output. */
  @Override
  public String toString() {
    return "RegisterRequest[username=" + username + ", password=<redacted>]";
  }
}
