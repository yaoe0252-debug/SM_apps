package com.smapps.chat.protocol.file;

import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.FileStatus;
import com.smapps.chat.protocol.validation.ProtocolValidationException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Rechecks identity, file state, and authorization expiry for every HTTP request. */
public final class FileAccessPolicy {

  private FileAccessPolicy() {}

  public static void authorizeUpload(
      UserId requesterId,
      UserId ownerId,
      FileStatus state,
      Instant authorizationExpiresAt,
      Clock clock) {
    requireUnexpired(authorizationExpiresAt, clock);
    if (!Objects.requireNonNull(ownerId).equals(requesterId)) {
      throw failure(ErrorCode.FORBIDDEN, "Only the file owner may upload content");
    }
    if (state != FileStatus.READY && state != FileStatus.UPLOADING) {
      throw failure(ErrorCode.CONFLICT, "File state does not allow upload");
    }
  }

  public static void authorizeDownload(
      UserId requesterId,
      UserId ownerId,
      UserId recipientId,
      FileStatus state,
      Instant authorizationExpiresAt,
      Clock clock) {
    requireUnexpired(authorizationExpiresAt, clock);
    boolean isParticipant =
        Objects.requireNonNull(requesterId).equals(Objects.requireNonNull(ownerId))
            || requesterId.equals(Objects.requireNonNull(recipientId));
    if (!isParticipant) {
      throw failure(ErrorCode.FORBIDDEN, "Only file participants may download content");
    }
    if (state != FileStatus.AVAILABLE) {
      throw failure(ErrorCode.CONFLICT, "File state does not allow download");
    }
  }

  private static void requireUnexpired(Instant expiresAt, Clock clock) {
    Objects.requireNonNull(clock, "clock must not be null");
    if (!Objects.requireNonNull(expiresAt).isAfter(clock.instant())) {
      throw failure(ErrorCode.FORBIDDEN, "File authorization has expired");
    }
  }

  private static ProtocolValidationException failure(ErrorCode code, String message) {
    return new ProtocolValidationException(code, message);
  }
}
