package com.smapps.chat.common.id;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies the stable textual form used for retry-safe message identity. */
class MessageIdTest {

  @Test
  void shouldRoundTripCanonicalValueWhenParsed() {
    String value = "30000000-0000-0000-0000-000000000001";

    assertEquals(value, MessageId.parse(value).toString());
  }
}
