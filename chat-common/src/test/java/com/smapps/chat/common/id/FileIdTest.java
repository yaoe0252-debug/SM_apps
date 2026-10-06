package com.smapps.chat.common.id;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies that file IDs remain independent from storage paths. */
class FileIdTest {

  @Test
  void shouldRoundTripCanonicalValueWhenParsed() {
    String value = "50000000-0000-0000-0000-000000000001";

    assertEquals(value, FileId.parse(value).toString());
  }
}
