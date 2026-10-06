package com.smapps.chat.protocol.validation;

import com.smapps.chat.common.id.FileId;
import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.auth.LoginRequest;
import com.smapps.chat.protocol.auth.LogoutRequest;
import com.smapps.chat.protocol.auth.RegisterRequest;
import com.smapps.chat.protocol.auth.ResumeRequest;
import com.smapps.chat.protocol.chat.ChatPrivateSend;
import com.smapps.chat.protocol.chat.ChatPublicSend;
import com.smapps.chat.protocol.chat.ChatTopicSend;
import com.smapps.chat.protocol.envelope.Envelope;
import com.smapps.chat.protocol.envelope.ProtocolVersion;
import com.smapps.chat.protocol.file.FileDeclare;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.FileStatus;
import com.smapps.chat.protocol.type.MessageType;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Central boundary validator reused by DTO constructors and server-side rechecks. */
public final class ProtocolValidator {

  private static final int CURSOR_MAX_UTF8_BYTES = 1024;
  private static final int IDENTIFIER_MAX_CODE_POINTS = 128;
  private static final int SESSION_TOKEN_MIN_CODE_POINTS = 32;
  private static final int SESSION_TOKEN_MAX_CODE_POINTS = 512;
  private static final int CLIENT_MESSAGE_MAX_CODE_POINTS = 256;
  private static final int TOPIC_ID_MAX_CODE_POINTS = 64;
  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
  private static final Pattern TOPIC_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
  private static final ProtocolValidator STANDARD =
      new ProtocolValidator(ProtocolLimits.defaults());

  private final ProtocolLimits limits;

  public ProtocolValidator(ProtocolLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits must not be null");
  }

  /** Initial validator used by records before server configuration is available. */
  public static ProtocolValidator standard() {
    return STANDARD;
  }

  public ProtocolLimits limits() {
    return limits;
  }

  public void validateCredentials(String username, String password) {
    validateUsername(username);
    validatePassword(password);
  }

  public void validateUsername(String username) {
    requireCodePointRange(
        username,
        "username",
        limits.usernameMinCodePoints(),
        limits.usernameMaxCodePoints(),
        false);
    requireUtf8BytesAtMost(username, "username", limits.usernameMaxUtf8Bytes());
  }

  public void validatePassword(String password) {
    requireCodePointRange(
        password,
        "password",
        limits.passwordMinCodePoints(),
        limits.passwordMaxCodePoints(),
        false);
  }

  public void validateSessionToken(String sessionToken) {
    requireCodePointRange(
        sessionToken,
        "sessionToken",
        SESSION_TOKEN_MIN_CODE_POINTS,
        SESSION_TOKEN_MAX_CODE_POINTS,
        false);
  }

  public void validateLoginResult(String sessionToken, Instant expiresAt, UserId userId) {
    validateSessionToken(sessionToken);
    requirePresent(expiresAt, "expiresAt");
    requirePresent(userId, "userId");
  }

  public void validateResumeRequest(String sessionToken, String cursor) {
    validateSessionToken(sessionToken);
    requireUtf8BytesAtMost(requirePresent(cursor, "cursor"), "cursor", CURSOR_MAX_UTF8_BYTES);
  }

  public void validateResumeResult(List<?> missingMessages, String nextCursor) {
    requirePresent(missingMessages, "missingMessages");
    if (missingMessages.stream().anyMatch(Objects::isNull)) {
      throw invalid("missingMessages must not contain null elements");
    }
    requireUtf8BytesAtMost(
        requirePresent(nextCursor, "nextCursor"), "nextCursor", CURSOR_MAX_UTF8_BYTES);
  }

  public void validateChatText(String text) {
    requireCodePointRange(text, "text", 1, limits.chatTextMaxCodePoints(), false);
  }

  public void validatePrivateChat(UserId recipientId, String text) {
    requirePresent(recipientId, "recipientId");
    validateChatText(text);
  }

  public void validateTopicChat(String topicId, String text) {
    requireCodePointRange(topicId, "topicId", 1, TOPIC_ID_MAX_CODE_POINTS, false);
    if (!TOPIC_ID.matcher(topicId).matches()) {
      throw invalid("topicId contains unsupported characters");
    }
    validateChatText(text);
  }

  public void validateAcceptedMessage(MessageId messageId, long serverSequence) {
    requirePresent(messageId, "messageId");
    requirePositive(serverSequence, "serverSequence");
  }

  public void validateChatEvent(
      String conversationId, UserId senderId, String text, long serverSequence) {
    requireCodePointRange(conversationId, "conversationId", 1, IDENTIFIER_MAX_CODE_POINTS, false);
    requirePresent(senderId, "senderId");
    validateChatText(text);
    requirePositive(serverSequence, "serverSequence");
  }

  public void validateFileDeclare(
      UserId recipientId, String displayName, long size, String sha256) {
    requirePresent(recipientId, "recipientId");
    validateDisplayName(displayName);
    validateFileSize(size);
    validateSha256(sha256);
  }

  public void validateDisplayName(String displayName) {
    requireCodePointRange(displayName, "displayName", 1, limits.displayNameMaxCodePoints(), false);
  }

  public void validateFileReady(FileId fileId, URI uploadUrl, Instant authorizationExpiresAt) {
    requirePresent(fileId, "fileId");
    requirePresent(uploadUrl, "uploadUrl");
    if (!"https".equalsIgnoreCase(uploadUrl.getScheme())) {
      throw invalid("uploadUrl must use HTTPS");
    }
    requirePresent(authorizationExpiresAt, "authorizationExpiresAt");
  }

  public void validateFileEvent(FileId fileId, FileStatus state, long size, String sha256) {
    requirePresent(fileId, "fileId");
    requirePresent(state, "state");
    validateFileSize(size);
    validateSha256(sha256);
  }

  public void validateErrorResponse(ErrorCode code, String message, MessageId requestId) {
    requirePresent(code, "code");
    requireCodePointRange(message, "message", 1, CLIENT_MESSAGE_MAX_CODE_POINTS, false);
    requirePresent(requestId, "requestId");
  }

  public void validateFileSize(long size) {
    if (size <= 0 || size > limits.fileMaxBytes()) {
      throw new ProtocolValidationException(
          ErrorCode.LIMIT_EXCEEDED,
          "size must be between 1 and " + limits.fileMaxBytes() + " bytes");
    }
  }

  public void validateSha256(String sha256) {
    requirePresent(sha256, "sha256");
    if (!SHA_256.matcher(sha256).matches()) {
      throw invalid("sha256 must be 64 lowercase hexadecimal characters");
    }
  }

  public void validateFrameSize(int utf8Bytes) {
    if (utf8Bytes <= 0 || utf8Bytes > limits.webSocketFrameMaxBytes()) {
      throw new ProtocolValidationException(
          ErrorCode.LIMIT_EXCEEDED,
          "WSS text frame exceeds " + limits.webSocketFrameMaxBytes() + " bytes");
    }
  }

  /** Rechecks direction, payload type, version, and session-derived sender identity. */
  public void validateClientEnvelope(Envelope<?> envelope, UserId authenticatedUserId) {
    requirePresent(envelope, "envelope");
    if (!ProtocolVersion.isSupported(envelope.protocolVersion())) {
      throw new ProtocolValidationException(
          ErrorCode.UNSUPPORTED_VERSION,
          "Unsupported protocolVersion: " + envelope.protocolVersion());
    }
    if (envelope.requestId() != null) {
      throw invalid("Client requests must not set requestId");
    }
    validateClientPayload(envelope.messageType(), envelope.payload());
    validateSender(envelope.messageType(), envelope.senderId(), authenticatedUserId);
  }

  private void validateClientPayload(MessageType messageType, Object payload) {
    Class<?> expectedType =
        switch (messageType) {
          case REGISTER_REQUEST -> RegisterRequest.class;
          case LOGIN_REQUEST -> LoginRequest.class;
          case LOGOUT_REQUEST -> LogoutRequest.class;
          case RESUME_REQUEST -> ResumeRequest.class;
          case CHAT_PUBLIC_SEND -> ChatPublicSend.class;
          case CHAT_PRIVATE_SEND -> ChatPrivateSend.class;
          case CHAT_TOPIC_SEND -> ChatTopicSend.class;
          case FILE_DECLARE -> FileDeclare.class;
          default -> throw invalid("messageType is not a client request: " + messageType);
        };
    if (!expectedType.isInstance(payload)) {
      throw invalid("payload does not match messageType " + messageType);
    }
  }

  private void validateSender(
      MessageType messageType, UserId senderId, UserId authenticatedUserId) {
    if (isAnonymousRequest(messageType)) {
      if (senderId != null) {
        throw new ProtocolValidationException(
            ErrorCode.FORBIDDEN, "Anonymous requests must not declare senderId");
      }
      return;
    }
    if (authenticatedUserId == null || senderId == null) {
      throw new ProtocolValidationException(
          ErrorCode.UNAUTHENTICATED, "An authenticated session is required");
    }
    if (!authenticatedUserId.equals(senderId)) {
      throw new ProtocolValidationException(
          ErrorCode.FORBIDDEN, "senderId does not match the authenticated session");
    }
  }

  private static boolean isAnonymousRequest(MessageType messageType) {
    return messageType == MessageType.REGISTER_REQUEST
        || messageType == MessageType.LOGIN_REQUEST
        || messageType == MessageType.RESUME_REQUEST;
  }

  private static void requireCodePointRange(
      String value, String name, int minimum, int maximum, boolean allowBlank) {
    requirePresent(value, name);
    int codePoints = value.codePointCount(0, value.length());
    if (codePoints < minimum || codePoints > maximum || (!allowBlank && value.isBlank())) {
      throw invalid(name + " length or content is outside the allowed range");
    }
  }

  private static void requireUtf8BytesAtMost(String value, String name, int maximum) {
    if (value.getBytes(StandardCharsets.UTF_8).length > maximum) {
      throw invalid(name + " exceeds " + maximum + " UTF-8 bytes");
    }
  }

  private static void requirePositive(long value, String name) {
    if (value <= 0) {
      throw invalid(name + " must be positive");
    }
  }

  private static <T> T requirePresent(T value, String name) {
    if (value == null) {
      throw invalid(name + " must not be null");
    }
    return value;
  }

  private static ProtocolValidationException invalid(String message) {
    return new ProtocolValidationException(ErrorCode.INVALID_INPUT, message);
  }
}
