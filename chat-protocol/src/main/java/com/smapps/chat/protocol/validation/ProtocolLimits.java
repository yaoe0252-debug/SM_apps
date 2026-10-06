package com.smapps.chat.protocol.validation;

/** Configurable protocol limits; the server remains authoritative at runtime. */
public record ProtocolLimits(
    int usernameMinCodePoints,
    int usernameMaxCodePoints,
    int usernameMaxUtf8Bytes,
    int passwordMinCodePoints,
    int passwordMaxCodePoints,
    int chatTextMaxCodePoints,
    int displayNameMaxCodePoints,
    long fileMaxBytes,
    int webSocketFrameMaxBytes) {

  private static final int MEBIBYTE = 1024 * 1024;

  public ProtocolLimits {
    requirePositive(usernameMinCodePoints, "usernameMinCodePoints");
    requireAtLeast(usernameMaxCodePoints, usernameMinCodePoints, "usernameMaxCodePoints");
    requirePositive(usernameMaxUtf8Bytes, "usernameMaxUtf8Bytes");
    requirePositive(passwordMinCodePoints, "passwordMinCodePoints");
    requireAtLeast(passwordMaxCodePoints, passwordMinCodePoints, "passwordMaxCodePoints");
    requirePositive(chatTextMaxCodePoints, "chatTextMaxCodePoints");
    requirePositive(displayNameMaxCodePoints, "displayNameMaxCodePoints");
    requirePositive(fileMaxBytes, "fileMaxBytes");
    requirePositive(webSocketFrameMaxBytes, "webSocketFrameMaxBytes");
  }

  /** Initial limits from the protocol completion guide. */
  public static ProtocolLimits defaults() {
    return new ProtocolLimits(3, 32, 64, 8, 128, 4096, 255, 20L * MEBIBYTE, 64 * 1024);
  }

  private static void requirePositive(long value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }

  private static void requireAtLeast(int value, int minimum, String name) {
    if (value < minimum) {
      throw new IllegalArgumentException(name + " must be at least " + minimum);
    }
  }
}
