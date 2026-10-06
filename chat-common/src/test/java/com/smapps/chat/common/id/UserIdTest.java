package com.smapps.chat.common.id;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies the stable textual form used across protocol boundaries. */
class UserIdTest {

  @Test
  void shouldRoundTripCanonicalValueWhenParsed() {
    String value = "20000000-0000-0000-0000-000000000001";

    assertEquals(value, UserId.parse(value).toString());
  }
}
