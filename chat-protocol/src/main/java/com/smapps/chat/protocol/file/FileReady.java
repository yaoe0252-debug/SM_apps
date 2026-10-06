package com.smapps.chat.protocol.file;

import com.smapps.chat.common.id.FileId;
import com.smapps.chat.protocol.validation.ProtocolValidator;
import java.net.URI;
import java.time.Instant;

/** Grants a time-limited HTTPS upload location for a declared file. */
public record FileReady(FileId fileId, URI uploadUrl, Instant authorizationExpiresAt) {

  public FileReady {
    ProtocolValidator.standard().validateFileReady(fileId, uploadUrl, authorizationExpiresAt);
  }
}
