package com.smapps.chat.protocol.file;

import com.smapps.chat.common.id.FileId;
import com.smapps.chat.protocol.type.FileStatus;
import com.smapps.chat.protocol.validation.ProtocolValidator;

/** Announces validated file state without placing binary data on RabbitMQ. */
public record FileEvent(FileId fileId, FileStatus state, long size, String sha256) {

  public FileEvent {
    ProtocolValidator.standard().validateFileEvent(fileId, state, size, sha256);
  }
}
