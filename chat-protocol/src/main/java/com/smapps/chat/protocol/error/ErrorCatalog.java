package com.smapps.chat.protocol.error;

import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.protocol.type.ErrorCode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Complete error catalog shared by HTTP responses and WSS ERROR frames. */
public final class ErrorCatalog {

  private static final Map<ErrorCode, ErrorDefinition> DEFINITIONS = createDefinitions();

  private ErrorCatalog() {}

  public static ErrorDefinition definition(ErrorCode code) {
    ErrorDefinition definition = DEFINITIONS.get(Objects.requireNonNull(code));
    if (definition == null) {
      throw new IllegalArgumentException("Missing error definition for " + code);
    }
    return definition;
  }

  /** Builds a response using only the catalog's client-safe message. */
  public static ErrorResponse response(ErrorCode code, MessageId requestId) {
    return new ErrorResponse(code, definition(code).userMessage(), requestId);
  }

  public static Map<ErrorCode, ErrorDefinition> definitions() {
    return DEFINITIONS;
  }

  private static Map<ErrorCode, ErrorDefinition> createDefinitions() {
    EnumMap<ErrorCode, ErrorDefinition> definitions = new EnumMap<>(ErrorCode.class);
    add(definitions, ErrorCode.INVALID_INPUT, 400, close(), never(), "请求内容不合法");
    add(definitions, ErrorCode.UNSUPPORTED_VERSION, 400, close(), never(), "协议版本不受支持");
    add(definitions, ErrorCode.UNSUPPORTED_MESSAGE_TYPE, 400, close(), never(), "消息类型不受支持");
    add(definitions, ErrorCode.UNAUTHENTICATED, 401, frame(), reauthenticate(), "请先登录");
    add(definitions, ErrorCode.SESSION_EXPIRED, 401, frame(), reauthenticate(), "登录已过期");
    add(definitions, ErrorCode.FORBIDDEN, 403, frame(), never(), "无权执行此操作");
    add(definitions, ErrorCode.NOT_FOUND, 404, frame(), never(), "请求的资源不存在");
    add(definitions, ErrorCode.USERNAME_TAKEN, 409, frame(), never(), "用户名已被使用");
    add(definitions, ErrorCode.INVALID_CREDENTIALS, 401, frame(), never(), "用户名或密码错误");
    add(definitions, ErrorCode.LIMIT_EXCEEDED, 413, frame(), never(), "请求超过允许限制");
    add(definitions, ErrorCode.CONFLICT, 409, frame(), stateRefresh(), "资源状态已发生变化");
    add(definitions, ErrorCode.RATE_LIMITED, 429, frame(), retry(), "请求过于频繁，请稍后重试");
    add(definitions, ErrorCode.TEMPORARY_UNAVAILABLE, 503, frame(), retry(), "服务暂时不可用，请稍后重试");
    add(definitions, ErrorCode.INTERNAL_ERROR, 500, frame(), retry(), "服务处理失败");
    if (definitions.size() != ErrorCode.values().length) {
      throw new IllegalStateException("Every ErrorCode must have one definition");
    }
    return Map.copyOf(definitions);
  }

  private static void add(
      Map<ErrorCode, ErrorDefinition> definitions,
      ErrorCode code,
      int httpStatus,
      WssFailureAction action,
      RetryPolicy retryPolicy,
      String userMessage) {
    definitions.put(code, new ErrorDefinition(httpStatus, action, retryPolicy, userMessage));
  }

  private static WssFailureAction frame() {
    return WssFailureAction.ERROR_FRAME;
  }

  private static WssFailureAction close() {
    return WssFailureAction.ERROR_FRAME_THEN_CLOSE;
  }

  private static RetryPolicy never() {
    return RetryPolicy.NEVER;
  }

  private static RetryPolicy reauthenticate() {
    return RetryPolicy.AFTER_REAUTHENTICATION;
  }

  private static RetryPolicy stateRefresh() {
    return RetryPolicy.AFTER_STATE_REFRESH;
  }

  private static RetryPolicy retry() {
    return RetryPolicy.WITH_BACKOFF_IF_IDEMPOTENT;
  }
}
