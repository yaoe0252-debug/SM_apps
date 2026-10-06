package com.smapps.chat.protocol.file;

import com.smapps.chat.common.id.FileId;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.FileStatus;
import com.smapps.chat.protocol.validation.ProtocolValidationException;
import com.smapps.chat.protocol.validation.ProtocolValidator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Objects;

/** HTTP metadata contract for streaming file content outside RabbitMQ. */
public final class FileTransferHttpContract {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String CONTENT_LENGTH_HEADER = "Content-Length";
  public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
  public static final String CONTENT_PATH_TEMPLATE = "/api/files/{fileId}/content";

  private static final String BEARER_PREFIX = "Bearer ";

  private FileTransferHttpContract() {}

  public static String contentPath(FileId fileId) {
    return "/api/files/" + Objects.requireNonNull(fileId) + "/content";
  }

  /** Requires one explicit bearer token without exposing it through a DTO. */
  public static String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      throw new ProtocolValidationException(
          ErrorCode.UNAUTHENTICATED, "A Bearer session token is required");
    }
    String token = authorizationHeader.substring(BEARER_PREFIX.length());
    ProtocolValidator.standard().validateSessionToken(token);
    return token;
  }

  /** Rejects chunked, truncated, or appended uploads before streaming begins. */
  public static void validateContentLength(long contentLength, long declaredSize) {
    ProtocolValidator.standard().validateFileSize(declaredSize);
    if (contentLength <= 0) {
      throw new ProtocolValidationException(ErrorCode.INVALID_INPUT, "Content-Length is required");
    }
    if (contentLength > declaredSize) {
      throw new ProtocolValidationException(
          ErrorCode.LIMIT_EXCEEDED, "Content-Length exceeds the declared size");
    }
    if (contentLength != declaredSize) {
      throw new ProtocolValidationException(
          ErrorCode.CONFLICT, "Content-Length differs from the declared size");
    }
  }

  /** A retry always replaces the old temporary file; byte streams are never concatenated. */
  public static UploadRetryAction uploadRetryAction(FileId fileId, FileStatus state) {
    Objects.requireNonNull(fileId, "fileId must not be null");
    if (state != FileStatus.READY && state != FileStatus.UPLOADING) {
      throw new ProtocolValidationException(
          ErrorCode.CONFLICT, "File state does not allow an upload retry");
    }
    return UploadRetryAction.REPLACE_TEMPORARY_FILE;
  }

  /** Produces a header value that cannot inject paths or additional headers. */
  public static String contentDisposition(String displayName) {
    String safeName = sanitizeDisplayName(displayName);
    String asciiFallback = safeName.replaceAll("[^A-Za-z0-9._-]", "_");
    if (asciiFallback.isBlank()) {
      asciiFallback = "download";
    }
    String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
    return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
  }

  static String sanitizeDisplayName(String displayName) {
    ProtocolValidator.standard().validateDisplayName(displayName);
    String normalized = Normalizer.normalize(displayName, Normalizer.Form.NFKC);
    String leafName = normalized.replace('\\', '/');
    leafName = leafName.substring(leafName.lastIndexOf('/') + 1);
    leafName = leafName.replaceAll("[\\p{Cntrl}\";]", "_").trim();
    if (leafName.isBlank() || ".".equals(leafName) || "..".equals(leafName)) {
      return "download";
    }
    return leafName;
  }
}
