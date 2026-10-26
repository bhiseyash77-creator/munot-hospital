package com.munot.hospital.util;

public final class Codes {
    private Codes() {}

    public static String of(String prefix, Long id) { return prefix + "-" + String.format("%06d", id); }
}
