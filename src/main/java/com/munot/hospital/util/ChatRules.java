package com.munot.hospital.util;

import com.munot.hospital.entity.Role;

public final class ChatRules {
    private ChatRules() {}

    public static final int MAX_LENGTH = 1000;
    public static final int MAX_PER_MINUTE = 30;

    /** Only doctors and receptionists use the staff chat. */
    public static boolean canUse(Role r) { return r == Role.DOCTOR || r == Role.RECEPTIONIST; }

    /** Trims, rejects empty or over-long text, and removes runs of 3+ blank lines. */
    public static String normalize(String body) {
        if (body == null) throw new IllegalArgumentException("Type a message first");
        String t = body.replace("\r\n", "\n").trim().replaceAll("\n{3,}", "\n\n");
        if (t.isEmpty()) throw new IllegalArgumentException("Type a message first");
        if (t.length() > MAX_LENGTH) throw new IllegalArgumentException("Message is too long (max " + MAX_LENGTH + " characters)");
        return t;
    }
}
