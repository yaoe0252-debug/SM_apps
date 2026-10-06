package com.smapps.chat.protocol.envelope;

/** Supported wire protocol versions. */
public final class ProtocolVersion {

  public static final String CURRENT = "1.0";

  private ProtocolVersion() {}

  /** Keeps version checks explicit so future migrations have one decision point. */
  public static boolean isSupported(String value) {
    return CURRENT.equals(value);
  }
}
