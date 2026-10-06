package com.smapps.chat.protocol.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.smapps.chat.common.id.FileId;
import com.smapps.chat.common.id.MessageId;
import com.smapps.chat.common.id.UserId;
import com.smapps.chat.protocol.type.ErrorCode;
import com.smapps.chat.protocol.type.FileStatus;
import com.smapps.chat.protocol.type.MessageType;
import java.io.IOException;

/** Jackson module that preserves the protocol's explicit text representations. */
final class ProtocolWireModule {

  private ProtocolWireModule() {}

  static SimpleModule create() {
    SimpleModule module = new SimpleModule("sm-apps-protocol-wire-types");
    addTextType(module, UserId.class, UserId::toString, UserId::parse);
    addTextType(module, MessageId.class, MessageId::toString, MessageId::parse);
    addTextType(module, FileId.class, FileId::toString, FileId::parse);
    addTextType(module, MessageType.class, MessageType::wireValue, MessageType::fromWireValue);
    addTextType(module, ErrorCode.class, ErrorCode::wireValue, ErrorCode::fromWireValue);
    addTextType(module, FileStatus.class, FileStatus::wireValue, FileStatus::fromWireValue);
    return module;
  }

  private static <T> void addTextType(
      SimpleModule module, Class<T> type, TextFormatter<T> formatter, TextParser<T> parser) {
    module.addSerializer(type, new TextValueSerializer<>(formatter));
    module.addDeserializer(type, new TextValueDeserializer<>(type, parser));
  }

  @FunctionalInterface
  private interface TextFormatter<T> {
    String format(T value);
  }

  @FunctionalInterface
  private interface TextParser<T> {
    T parse(String value);
  }

  private static final class TextValueSerializer<T> extends JsonSerializer<T> {

    private final TextFormatter<T> formatter;

    private TextValueSerializer(TextFormatter<T> formatter) {
      this.formatter = formatter;
    }

    @Override
    public void serialize(T value, JsonGenerator generator, SerializerProvider serializers)
        throws IOException {
      generator.writeString(formatter.format(value));
    }
  }

  private static final class TextValueDeserializer<T> extends JsonDeserializer<T> {

    private final Class<T> type;
    private final TextParser<T> parser;

    private TextValueDeserializer(Class<T> type, TextParser<T> parser) {
      this.type = type;
      this.parser = parser;
    }

    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
      if (!jsonParser.hasToken(JsonToken.VALUE_STRING)) {
        throw JsonMappingException.from(jsonParser, type.getSimpleName() + " must be a string");
      }
      try {
        return parser.parse(jsonParser.getText());
      } catch (IllegalArgumentException exception) {
        throw JsonMappingException.from(
            jsonParser, "Invalid " + type.getSimpleName() + " value", exception);
      }
    }
  }
}
