package com.smapps.chat.protocol.file;

import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Declares file metadata before any binary content is uploaded. */
public record FileDeclare(UserId recipientId, String displayName, long size, String sha256) {

  public FileDeclare {
    displayName = FileTransferHttpContract.sanitizeDisplayName(displayName);
    ProtocolValidator.standard().validateFileDeclare(recipientId, displayName, size, sha256);
  }
}
