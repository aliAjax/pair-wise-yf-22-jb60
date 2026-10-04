package com.generated.qualityTrace.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;

/**
 * 极简 JSON 工具，封装 Jackson ObjectMapper。
 */
public final class JsonUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  private JsonUtils() {
  }

  public static String write(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("write json failed", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> read(String json) {
    try {
      return MAPPER.readValue(json, Map.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("read json failed", e);
    }
  }

  public static <T> T read(String json, Class<T> type) {
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalArgumentException("read json failed", e);
    }
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }
}
