package com.smapps.chat.protocol.auth;

import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Requests a new authenticated session. */
public record LoginRequest(String username, String password) {

  public LoginRequest {
    ProtocolValidator.standard().validateCredentials(username, password);
  }

  /** Keeps passwords out of accidental diagnostic output. */
  @Override
  public String toString() {
    return "LoginRequest[username=" + username + ", password=<redacted>]";
  }
}
