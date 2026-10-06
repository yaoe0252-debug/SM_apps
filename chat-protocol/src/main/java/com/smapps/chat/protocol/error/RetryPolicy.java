package com.smapps.chat.protocol.error;

/** Client retry guidance attached to each stable protocol error. */
public enum RetryPolicy {
  NEVER,
  AFTER_REAUTHENTICATION,
  AFTER_STATE_REFRESH,
  WITH_BACKOFF_IF_IDEMPOTENT
}
