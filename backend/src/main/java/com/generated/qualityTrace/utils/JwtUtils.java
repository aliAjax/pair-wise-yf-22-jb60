package com.generated.qualityTrace.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 极简 JWT（JWS）工具：HMAC-SHA256 签名，无第三方依赖。
 *
 * <p>token 结构为 header.payload.signature，payload 含 uid、username、role、exp。
 * 生产环境可替换为标准库，这里保持自包含以便在任意环境运行。</p>
 */
public final class JwtUtils {

  private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

  private JwtUtils() {
  }

  /** 生成 token。ttlMillis 为有效期毫秒数。 */
  public static String issue(Long uid, String username, String role, String secret, long ttlMillis) {
    long now = Instant.now().getEpochSecond();
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("uid", uid);
    payload.put("username", username);
    payload.put("role", role);
    payload.put("iat", now);
    payload.put("exp", now + ttlMillis / 1000);
    String h = B64.encodeToString(JsonUtils.write(header).getBytes(StandardCharsets.UTF_8));
    String p = B64.encodeToString(JsonUtils.write(payload).getBytes(StandardCharsets.UTF_8));
    String sig = sign(h + "." + p, secret);
    return h + "." + p + "." + sig;
  }

  /** 解析并校验 token；非法/过期抛出 {@link JwtException}。 */
  public static Map<String, Object> parse(String token, String secret) {
    if (token == null || token.isBlank()) {
      throw new JwtException("empty token", false);
    }
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new JwtException("malformed token", false);
    }
    String expectedSig = sign(parts[0] + "." + parts[1], secret);
    if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8),
        parts[2].getBytes(StandardCharsets.UTF_8))) {
      throw new JwtException("bad signature", false);
    }
    Map<String, Object> payload;
    try {
      payload = JsonUtils.read(new String(B64_DEC.decode(parts[1]), StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new JwtException("bad payload", false);
    }
    Object exp = payload.get("exp");
    if (exp instanceof Number n && n.longValue() < Instant.now().getEpochSecond()) {
      throw new JwtException("token expired", true);
    }
    return payload;
  }

  private static String sign(String content, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return B64.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("HMAC-SHA256 unavailable", e);
    }
  }

  /** 从 payload 取 Long 型 uid。 */
  public static Long uid(Map<String, Object> payload) {
    Object v = payload.get("uid");
    if (v instanceof Number n) {
      return n.longValue();
    }
    return v == null ? null : Long.valueOf(v.toString());
  }

  /** token 非法或过期时抛出；expired 区分是否过期。 */
  public static class JwtException extends RuntimeException {
    private final boolean expired;

    public JwtException(String message, boolean expired) {
      super(message);
      this.expired = expired;
    }

    public boolean isExpired() {
      return expired;
    }
  }
}
