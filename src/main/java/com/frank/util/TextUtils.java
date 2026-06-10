package com.frank.util;

public final class TextUtils {

    private TextUtils() {
    }

    /**
     * Converts input to sentence case: first character upper, remainder lower.
     * Example: "kenya" -> "Kenya", "TANZANIA" -> "Tanzania"
     */
    public static String toSentenceCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String trimmed = input.trim();
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase();
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }
}
