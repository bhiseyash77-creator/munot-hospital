package com.munot.hospital.util;

public final class Phones {
    private Phones() {}

    /** Keeps digits only; strips a leading +91 / 91 / 0 for 10-digit Indian numbers. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("\\D", "");
        if (d.length() == 12 && d.startsWith("91")) d = d.substring(2);
        if (d.length() == 11 && d.startsWith("0")) d = d.substring(1);
        return d;
    }

    /** E.164-style digits for messaging providers (defaults to India +91). */
    public static String international(String normalized) {
        if (normalized == null) return null;
        return normalized.length() == 10 ? "91" + normalized : normalized;
    }
}
