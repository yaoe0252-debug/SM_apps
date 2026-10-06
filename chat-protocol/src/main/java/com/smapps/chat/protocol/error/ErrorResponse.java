package com.smapps.chat.protocol.error;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Client-safe failure response correlated to the rejected request. */
public record ErrorResponse(ErrorCode code, String message, MessageId requestId) {

  public ErrorResponse {
    ProtocolValidator.standard().validateErrorResponse(code, message, requestId);
  }
}
