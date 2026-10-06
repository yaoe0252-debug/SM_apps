package com.smapps.chat.protocol.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.protocol.type.ErrorCode;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

/** Verifies complete, client-safe transport mappings for stable errors. */
class ErrorCatalogTest {

  @Test
  void shouldDefineTransportMappingWhenAnyErrorCodeIsUsed() {
    assertEquals(EnumSet.allOf(ErrorCode.class), ErrorCatalog.definitions().keySet());
  }

  @Test
  void shouldUseSafeCatalogMessageWhenErrorResponseIsBuilt() {
    MessageId requestId = MessageId.parse("10000000-0000-0000-0000-000000000001");

    ErrorResponse response = ErrorCatalog.response(ErrorCode.INTERNAL_ERROR, requestId);

    assertEquals(requestId, response.requestId());
    assertFalse(response.message().contains("SQL"));
    assertFalse(response.message().contains("Exception"));
    assertFalse(response.message().contains("\\"));
  }
}
