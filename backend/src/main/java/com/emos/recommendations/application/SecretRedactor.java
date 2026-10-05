package com.emos.recommendations.application;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SecretRedactor {
  private static final Pattern BEARER =
      Pattern.compile("(?i)(authorization\\s*:\\s*bearer\\s+)[^\\s]+");
  private static final Pattern KEY_VALUE =
      Pattern.compile("(?i)(api[_-]?key|password|token|secret)\\s*[:=]\\s*[^\\s]+");
  private static final Pattern CONNECTION_CREDENTIALS =
      Pattern.compile("(jdbc:[^:]+://[^:/@\\s]+:)[^@/\\s]+(@)");

  public String redact(String value) {
    if (value == null) return null;
    var redacted = BEARER.matcher(value).replaceAll("$1[REDACTED]");
    redacted = KEY_VALUE.matcher(redacted).replaceAll("$1=[REDACTED]");
    return CONNECTION_CREDENTIALS.matcher(redacted).replaceAll("$1[REDACTED]$2");
  }
}
