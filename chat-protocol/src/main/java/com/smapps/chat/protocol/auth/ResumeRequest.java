package com.smapps.chat.protocol.auth;

import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Restores a session and requests messages after a server cursor. */
public record ResumeRequest(String sessionToken, String cursor) {

  public ResumeRequest {
    ProtocolValidator.standard().validateResumeRequest(sessionToken, cursor);
  }

  @Override
  public String toString() {
    return "ResumeRequest[sessionToken=<redacted>, cursor=" + cursor + "]";
  }
}
