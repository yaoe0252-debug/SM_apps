package com.smapps.chat.protocol.type;

import com.smapps.chat.protocol.validation.ProtocolValidationException;
import java.util.Arrays;

/** Stable external names for protocol payload types. */
public enum MessageType {
  REGISTER_REQUEST("REGISTER_REQUEST"),
  REGISTER_RESULT("REGISTER_RESULT"),
  LOGIN_REQUEST("LOGIN_REQUEST"),
  LOGIN_RESULT("LOGIN_RESULT"),
  LOGOUT_REQUEST("LOGOUT_REQUEST"),
  LOGOUT_RESULT("LOGOUT_RESULT"),
  RESUME_REQUEST("RESUME_REQUEST"),
  RESUME_RESULT("RESUME_RESULT"),
  CHAT_PUBLIC_SEND("CHAT_PUBLIC_SEND"),
  CHAT_PRIVATE_SEND("CHAT_PRIVATE_SEND"),
  CHAT_TOPIC_SEND("CHAT_TOPIC_SEND"),
  CHAT_ACCEPTED("CHAT_ACCEPTED"),
  CHAT_EVENT("CHAT_EVENT"),
  FILE_DECLARE("FILE_DECLARE"),
  FILE_READY("FILE_READY"),
  FILE_EVENT("FILE_EVENT"),
  ERROR("ERROR");

  private final String wireValue;

  MessageType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  /** Rejects unknown values without silently mapping them to an existing meaning. */
  public static MessageType fromWireValue(String value) {
    return Arrays.stream(values())
        .filter(type -> type.wireValue.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new ProtocolValidationException(
                    ErrorCode.UNSUPPORTED_MESSAGE_TYPE, "Unknown messageType: " + value));
  }
}
