package com.emos.platform.safety;

import tools.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SafeContent {
    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "authorization", "token", "secret", "password", "rawpayload", "rawbody");
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i)(bearer\\s+\\S+|ghp_[a-z0-9]{20,}|sk-[a-z0-9_-]{20,}|authorization\\s*:|[a-z0-9_.]+(?:exception|error))");

    private SafeContent() {
    }

    public static void requireSafe(JsonNode details) {
        if (details == null || containsUnsafe(details)) {
            throw new IllegalArgumentException("Audit details contain unsafe content");
        }
    }

    public static String safeMessage(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > 300
                || candidate.startsWith("{") || candidate.startsWith("[")
                || SENSITIVE_VALUE.matcher(candidate).find()) {
            return "Integration request failed";
        }
        return candidate.strip();
    }

    private static boolean containsUnsafe(JsonNode node) {
        if (node.isObject()) {
            var fields = node.properties().iterator();
            while (fields.hasNext()) {
                var field = fields.next();
                var normalized = field.getKey().replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);
                if (FORBIDDEN_FIELDS.contains(normalized) || containsUnsafe(field.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (node.isArray()) {
            for (var child : node) {
                if (containsUnsafe(child)) return true;
            }
            return false;
        }
        return node.isString() && SENSITIVE_VALUE.matcher(node.stringValue()).find();
    }
}
