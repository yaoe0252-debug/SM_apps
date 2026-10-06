package com.smapps.chat.protocol.codec;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.auth.LoginRequest;
import com.smapps.chat.protocol.auth.LoginResult;
import com.smapps.chat.protocol.auth.LogoutRequest;
import com.smapps.chat.protocol.auth.LogoutResult;
import com.smapps.chat.protocol.auth.RegisterRequest;
import com.smapps.chat.protocol.auth.RegisterResult;
import com.smapps.chat.protocol.auth.ResumeRequest;
import com.smapps.chat.protocol.auth.ResumeResult;
import com.smapps.chat.protocol.chat.ChatAccepted;
import com.smapps.chat.protocol.chat.ChatEvent;
import com.smapps.chat.protocol.chat.ChatPrivateSend;
import com.smapps.chat.protocol.chat.ChatPublicSend;
import com.smapps.chat.protocol.chat.ChatTopicSend;
import com.smapps.chat.protocol.envelope.Envelope;
import com.smapps.chat.protocol.envelope.ProtocolVersion;
import com.smapps.chat.protocol.error.ErrorResponse;
import com.smapps.chat.protocol.file.FileDeclare;
import com.smapps.chat.protocol.file.FileEvent;
import com.smapps.chat.protocol.file.FileReady;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.MessageType;
import com.smapps.chat.protocol.validation.ProtocolValidationException;
import com.smapps.chat.protocol.validation.ProtocolValidator;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Strict UTF-8 JSON codec with bounded parsing and explicit payload dispatch. */
public final class ProtocolCodec {

  private static final int MAX_NESTING_DEPTH = 20;
  private static final int MAX_ARRAY_ELEMENTS = 100;
  private static final int MAX_OBJECT_FIELDS = 64;
  private static final int MAX_STRING_LENGTH = 64 * 1024;
  private static final Set<String> ENVELOPE_FIELDS =
      Set.of(
          "protocolVersion",
          "messageId",
          "messageType",
          "senderId",
          "timestamp",
          "payload",
          "requestId");
  private static final Set<String> TYPE_MARKER_FIELDS =
      Set.of("@class", "@type", "$type", "_class", "javaClass");

  private final ObjectMapper objectMapper;
  private final ProtocolValidator validator;
  private final Map<MessageType, Class<?>> payloadTypes;
  private final Map<MessageType, Set<String>> payloadFields;

  public ProtocolCodec() {
    this(ProtocolValidator.standard());
  }

  public ProtocolCodec(ProtocolValidator validator) {
    this.validator = Objects.requireNonNull(validator, "validator must not be null");
    this.objectMapper = createObjectMapper();
    this.payloadTypes = createPayloadTypes();
    this.payloadFields = createPayloadFields(payloadTypes);
  }

  /** Encodes one complete envelope and enforces the configured WSS frame limit. */
  public byte[] encode(Envelope<?> envelope) {
    Objects.requireNonNull(envelope, "envelope must not be null");
    validateVersion(envelope.protocolVersion());
    validatePayloadType(envelope.messageType(), envelope.payload());
    try {
      byte[] encoded = objectMapper.writeValueAsBytes(envelope);
      validator.validateFrameSize(encoded.length);
      return encoded;
    } catch (ProtocolValidationException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalid("The envelope could not be encoded", exception);
    }
  }

  /** Decodes a complete WSS text frame after byte, UTF-8, and structure checks. */
  public Envelope<?> decode(byte[] frame) {
    if (frame == null) {
      throw invalid("Frame must not be null");
    }
    validator.validateFrameSize(frame.length);
    String json = decodeUtf8(frame);
    try {
      JsonNode root = objectMapper.readTree(json);
      if (root == null || !root.isObject()) {
        throw invalid("Envelope must be a JSON object");
      }
      validateBoundedTree(root);
      requireFields(root, ENVELOPE_FIELDS, "envelope");

      String protocolVersion = requireText(root, "protocolVersion");
      validateVersion(protocolVersion);
      MessageType messageType = MessageType.fromWireValue(requireText(root, "messageType"));

      JsonNode payloadNode = root.get("payload");
      if (!payloadNode.isObject()) {
        throw invalid("payload must be a JSON object");
      }
      requireFields(payloadNode, payloadFields.get(messageType), "payload");
      Object payload = objectMapper.treeToValue(payloadNode, payloadTypes.get(messageType));

      return new Envelope<>(
          protocolVersion,
          MessageId.parse(requireText(root, "messageId")),
          messageType,
          nullableUserId(root.get("senderId")),
          parseUtcInstant(requireText(root, "timestamp")),
          payload,
          nullableMessageId(root.get("requestId")));
    } catch (ProtocolValidationException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalid("Invalid JSON protocol frame", exception);
    }
  }

  /** Adds session-derived sender validation for inbound client frames. */
  public Envelope<?> decodeClientFrame(byte[] frame, UserId authenticatedUserId) {
    Envelope<?> envelope = decode(frame);
    validator.validateClientEnvelope(envelope, authenticatedUserId);
    return envelope;
  }

  private ObjectMapper createObjectMapper() {
    StreamReadConstraints constraints =
        StreamReadConstraints.builder()
            .maxNestingDepth(MAX_NESTING_DEPTH)
            .maxStringLength(MAX_STRING_LENGTH)
            .maxNumberLength(64)
            .maxNameLength(128)
            .build();
    JsonFactory jsonFactory =
        JsonFactory.builder()
            .streamReadConstraints(constraints)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .disable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .disable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();
    return JsonMapper.builder(jsonFactory)
        .addModule(new JavaTimeModule())
        .addModule(ProtocolWireModule.create())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .build();
  }

  private static Map<MessageType, Class<?>> createPayloadTypes() {
    EnumMap<MessageType, Class<?>> types = new EnumMap<>(MessageType.class);
    types.put(MessageType.REGISTER_REQUEST, RegisterRequest.class);
    types.put(MessageType.REGISTER_RESULT, RegisterResult.class);
    types.put(MessageType.LOGIN_REQUEST, LoginRequest.class);
    types.put(MessageType.LOGIN_RESULT, LoginResult.class);
    types.put(MessageType.LOGOUT_REQUEST, LogoutRequest.class);
    types.put(MessageType.LOGOUT_RESULT, LogoutResult.class);
    types.put(MessageType.RESUME_REQUEST, ResumeRequest.class);
    types.put(MessageType.RESUME_RESULT, ResumeResult.class);
    types.put(MessageType.CHAT_PUBLIC_SEND, ChatPublicSend.class);
    types.put(MessageType.CHAT_PRIVATE_SEND, ChatPrivateSend.class);
    types.put(MessageType.CHAT_TOPIC_SEND, ChatTopicSend.class);
    types.put(MessageType.CHAT_ACCEPTED, ChatAccepted.class);
    types.put(MessageType.CHAT_EVENT, ChatEvent.class);
    types.put(MessageType.FILE_DECLARE, FileDeclare.class);
    types.put(MessageType.FILE_READY, FileReady.class);
    types.put(MessageType.FILE_EVENT, FileEvent.class);
    types.put(MessageType.ERROR, ErrorResponse.class);
    if (types.size() != MessageType.values().length) {
      throw new IllegalStateException("Every MessageType must have one payload record");
    }
    return Map.copyOf(types);
  }

  private static Map<MessageType, Set<String>> createPayloadFields(
      Map<MessageType, Class<?>> payloadTypes) {
    EnumMap<MessageType, Set<String>> fields = new EnumMap<>(MessageType.class);
    payloadTypes.forEach(
        (messageType, payloadType) ->
            fields.put(
                messageType,
                Arrays.stream(payloadType.getRecordComponents())
                    .map(component -> component.getName())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet())));
    return Map.copyOf(fields);
  }

  private static String decodeUtf8(byte[] frame) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(frame))
          .toString();
    } catch (CharacterCodingException exception) {
      throw invalid("Frame is not valid UTF-8", exception);
    }
  }

  private static void validateBoundedTree(JsonNode node) {
    if (node.isArray() && node.size() > MAX_ARRAY_ELEMENTS) {
      throw invalid("JSON array exceeds " + MAX_ARRAY_ELEMENTS + " elements");
    }
    if (node.isObject()) {
      if (node.size() > MAX_OBJECT_FIELDS) {
        throw invalid("JSON object exceeds " + MAX_OBJECT_FIELDS + " fields");
      }
      node.fieldNames()
          .forEachRemaining(
              name -> {
                if (TYPE_MARKER_FIELDS.contains(name)) {
                  throw invalid("Polymorphic type marker is not allowed");
                }
              });
    }
    node.elements().forEachRemaining(ProtocolCodec::validateBoundedTree);
  }

  private static void requireFields(JsonNode object, Set<String> required, String context) {
    Set<String> missing = new HashSet<>();
    required.forEach(
        field -> {
          if (!object.has(field)) {
            missing.add(field);
          }
        });
    if (!missing.isEmpty()) {
      throw invalid(context + " is missing required fields: " + missing);
    }
  }

  private void validatePayloadType(MessageType messageType, Object payload) {
    Objects.requireNonNull(messageType, "messageType must not be null");
    Objects.requireNonNull(payload, "payload must not be null");
    Class<?> expected = payloadTypes.get(messageType);
    if (expected == null || !expected.isInstance(payload)) {
      throw invalid("payload does not match messageType " + messageType);
    }
  }

  private static String requireText(JsonNode object, String field) {
    JsonNode value = object.get(field);
    if (value == null || !value.isTextual()) {
      throw invalid(field + " must be a string");
    }
    return value.textValue();
  }

  private static UserId nullableUserId(JsonNode value) {
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isTextual()) {
      throw invalid("senderId must be a UUID string or null");
    }
    return UserId.parse(value.textValue());
  }

  private static MessageId nullableMessageId(JsonNode value) {
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isTextual()) {
      throw invalid("requestId must be a UUID string or null");
    }
    return MessageId.parse(value.textValue());
  }

  private static Instant parseUtcInstant(String value) {
    if (!value.endsWith("Z")) {
      throw invalid("timestamp must use ISO-8601 UTC with a Z suffix");
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException exception) {
      throw invalid("timestamp is not a valid ISO-8601 Instant", exception);
    }
  }

  private static void validateVersion(String version) {
    if (!ProtocolVersion.isSupported(version)) {
      throw new ProtocolValidationException(
          ErrorCode.UNSUPPORTED_VERSION, "Unsupported protocolVersion: " + version);
    }
  }

  private static ProtocolValidationException invalid(String message) {
    return new ProtocolValidationException(ErrorCode.INVALID_INPUT, message);
  }

  private static ProtocolValidationException invalid(String message, Exception cause) {
    ProtocolValidationException exception = invalid(message);
    exception.initCause(cause);
    return exception;
  }
}
