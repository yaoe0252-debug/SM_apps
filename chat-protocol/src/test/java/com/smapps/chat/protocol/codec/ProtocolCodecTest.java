package com.smapps.chat.protocol.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.chat.ChatPublicSend;
import com.smapps.chat.protocol.envelope.Envelope;
import com.smapps.chat.protocol.envelope.ProtocolVersion;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.MessageType;
import com.smapps.chat.protocol.validation.ProtocolValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exercises strict parsing, compatibility, and all protocol 1.0 golden frames. */
class ProtocolCodecTest {

  private static final String EXAMPLE_ROOT = "/examples/protocol-v1/";
  private static final List<String> GOLDEN_FILES =
      List.of(
          "register-request.json",
          "register-result.json",
          "login-request.json",
          "login-result.json",
          "logout-request.json",
          "logout-result.json",
          "resume-request.json",
          "resume-result.json",
          "chat-public-send.json",
          "chat-private-send.json",
          "chat-topic-send.json",
          "chat-accepted.json",
          "chat-event.json",
          "file-declare.json",
          "file-ready.json",
          "file-event.json",
          "error.json");

  private final ProtocolCodec codec = new ProtocolCodec();

  @Test
  void shouldRoundTripEveryMessageWhenGoldenFixturesAreLoaded() throws IOException {
    Set<MessageType> decodedTypes = EnumSet.noneOf(MessageType.class);

    for (String fileName : GOLDEN_FILES) {
      Envelope<?> decoded = codec.decode(readExample(fileName));
      Envelope<?> roundTripped = codec.decode(codec.encode(decoded));
      assertEquals(decoded, roundTripped, fileName);
      decodedTypes.add(decoded.messageType());
    }

    assertEquals(EnumSet.allOf(MessageType.class), decodedTypes);
  }

  @Test
  void shouldAcceptFrameWhenFieldOrderChanges() {
    String reordered =
        """
        {
          "payload":{"text":"hello"},
          "timestamp":"2026-10-05T08:00:00Z",
          "senderId":"20000000-0000-0000-0000-000000000001",
          "messageType":"CHAT_PUBLIC_SEND",
          "messageId":"30000000-0000-0000-0000-000000000001",
          "requestId":null,
          "protocolVersion":"1.0"
        }
        """;

    Envelope<?> decoded = codec.decode(utf8(reordered));

    assertInstanceOf(ChatPublicSend.class, decoded.payload());
  }

  @Test
  void shouldIgnoreUnknownOptionalFieldsWhenFrameIsOtherwiseValid() {
    String compatible =
        validChatJson()
            .replace("\"payload\":{", "\"futureTopLevel\":true,\"payload\":{")
            .replace("\"text\":\"hello\"", "\"text\":\"hello\",\"futurePayload\":7");

    Envelope<?> decoded = codec.decode(utf8(compatible));

    assertEquals(new ChatPublicSend("hello"), decoded.payload());
  }

  @Test
  void shouldRejectFrameWhenRequiredFieldIsMissing() {
    String missingMessageId =
        validChatJson().replace("\"messageId\":\"30000000-0000-0000-0000-000000000001\",", "");

    assertInvalidInput(missingMessageId);
  }

  @Test
  void shouldRejectFrameWhenPayloadExceedsBoundary() {
    String oversized = validChatJson().replace("hello", "a".repeat(4097));

    assertInvalidInput(oversized);
  }

  @Test
  void shouldRejectFrameWhenMessageTypeIsUnknown() {
    String unknownType = validChatJson().replace("CHAT_PUBLIC_SEND", "FUTURE_MESSAGE");

    ProtocolValidationException exception =
        assertThrows(ProtocolValidationException.class, () -> codec.decode(utf8(unknownType)));

    assertEquals(ErrorCode.UNSUPPORTED_MESSAGE_TYPE, exception.errorCode());
  }

  @Test
  void shouldRejectFrameWhenMajorVersionIsUnsupported() {
    String unsupported = validChatJson().replace("\"1.0\"", "\"2.0\"");

    ProtocolValidationException exception =
        assertThrows(ProtocolValidationException.class, () -> codec.decode(utf8(unsupported)));

    assertEquals(ErrorCode.UNSUPPORTED_VERSION, exception.errorCode());
  }

  @Test
  void shouldRejectFrameWhenUtf8IsMalformed() {
    byte[] malformedUtf8 = {(byte) 0xC3, (byte) 0x28};

    ProtocolValidationException exception =
        assertThrows(ProtocolValidationException.class, () -> codec.decode(malformedUtf8));

    assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode());
  }

  @Test
  void shouldRejectFrameWhenJsonIsMalformed() {
    assertInvalidInput("{\"protocolVersion\":\"1.0\"");
  }

  @Test
  void shouldRejectFrameWhenJsonFieldIsDuplicated() {
    String duplicate =
        validChatJson()
            .replace(
                "\"protocolVersion\":\"1.0\"",
                "\"protocolVersion\":\"1.0\",\"protocolVersion\":\"1.0\"");

    assertInvalidInput(duplicate);
  }

  @Test
  void shouldRejectFrameWhenUnknownArrayIsUnbounded() {
    String oversizedArray =
        validChatJson()
            .replace("\"payload\":{", "\"futureValues\":[" + "0,".repeat(100) + "0],\"payload\":{");

    assertInvalidInput(oversizedArray);
  }

  @Test
  void shouldRejectFrameWhenNestingIsTooDeep() {
    String nestedValue = "{}";
    for (int index = 0; index < 25; index++) {
      nestedValue = "{\"level\":" + nestedValue + "}";
    }
    String deeplyNested =
        validChatJson().replace("\"payload\":{", "\"future\":" + nestedValue + ",\"payload\":{");

    assertInvalidInput(deeplyNested);
  }

  @Test
  void shouldRejectFrameWhenPolymorphicTypeMarkerAppears() {
    String typeMarker =
        validChatJson().replace("\"text\":\"hello\"", "\"text\":\"hello\",\"@class\":\"X\"");

    assertInvalidInput(typeMarker);
  }

  @Test
  void shouldRejectEnvelopeWhenPayloadTypeDoesNotMatchMessageType() {
    Envelope<ChatPublicSend> invalidEnvelope =
        new Envelope<>(
            ProtocolVersion.CURRENT,
            MessageId.parse("30000000-0000-0000-0000-000000000001"),
            MessageType.LOGIN_REQUEST,
            UserId.parse("20000000-0000-0000-0000-000000000001"),
            Instant.parse("2026-10-05T08:00:00Z"),
            new ChatPublicSend("hello"),
            null);

    assertThrows(ProtocolValidationException.class, () -> codec.encode(invalidEnvelope));
  }

  private static byte[] readExample(String fileName) throws IOException {
    try (InputStream input = ProtocolCodecTest.class.getResourceAsStream(EXAMPLE_ROOT + fileName)) {
      if (input == null) {
        throw new IOException("Missing golden fixture " + fileName);
      }
      return input.readAllBytes();
    }
  }

  private void assertInvalidInput(String json) {
    ProtocolValidationException exception =
        assertThrows(ProtocolValidationException.class, () -> codec.decode(utf8(json)));
    assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode());
  }

  private static byte[] utf8(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static String validChatJson() {
    return """
        {
          "protocolVersion":"1.0",
          "messageId":"30000000-0000-0000-0000-000000000001",
          "messageType":"CHAT_PUBLIC_SEND",
          "senderId":"20000000-0000-0000-0000-000000000001",
          "timestamp":"2026-10-05T08:00:00Z",
          "payload":{"text":"hello"},
          "requestId":null
        }
        """;
  }
}
