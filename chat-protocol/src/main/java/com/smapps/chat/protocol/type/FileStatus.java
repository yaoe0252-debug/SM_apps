package com.smapps.chat.protocol.type;

import com.smapps.chat.protocol.validation.ProtocolValidationException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Monotonic file transfer states used by FILE_EVENT. */
public enum FileStatus {
  DECLARED("DECLARED"),
  READY("READY"),
  UPLOADING("UPLOADING"),
  AVAILABLE("AVAILABLE"),
  FAILED("FAILED"),
  CANCELLED("CANCELLED"),
  EXPIRED("EXPIRED");

  private static final Map<FileStatus, Set<FileStatus>> TRANSITIONS =
      Map.of(
          DECLARED,
          EnumSet.of(READY, FAILED, CANCELLED, EXPIRED),
          READY,
          EnumSet.of(UPLOADING, FAILED, CANCELLED, EXPIRED),
          UPLOADING,
          EnumSet.of(AVAILABLE, FAILED, CANCELLED, EXPIRED),
          AVAILABLE,
          EnumSet.noneOf(FileStatus.class),
          FAILED,
          EnumSet.noneOf(FileStatus.class),
          CANCELLED,
          EnumSet.noneOf(FileStatus.class),
          EXPIRED,
          EnumSet.noneOf(FileStatus.class));

  private final String wireValue;

  FileStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  /** Rejects unknown states instead of accepting an invalid transition target. */
  public static FileStatus fromWireValue(String value) {
    return Arrays.stream(values())
        .filter(status -> status.wireValue.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new ProtocolValidationException(
                    ErrorCode.UNSUPPORTED_MESSAGE_TYPE, "Unknown file status: " + value));
  }

  /** Prevents a delayed event from moving a completed transfer backwards. */
  public boolean canTransitionTo(FileStatus next) {
    return next != null && TRANSITIONS.get(this).contains(next);
  }
}
