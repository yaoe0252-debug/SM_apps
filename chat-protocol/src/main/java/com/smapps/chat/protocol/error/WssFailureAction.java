package com.smapps.chat.protocol.error;

/** WSS action used after a safe ERROR payload is emitted. */
public enum WssFailureAction {
  ERROR_FRAME,
  ERROR_FRAME_THEN_CLOSE
}
