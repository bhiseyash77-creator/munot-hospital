package com.munot.hospital.util;

import java.util.Map;

/** Shared image-upload checks: allowed types and a magic-byte check so a renamed file is rejected. */
public final class ImageRules {
    private ImageRules() {}

    private static final Map<String, String> TYPES = Map.of("jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    public static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }

    /** Content type for an allowed extension, or null if the extension is not allowed. */
    public static String contentTypeFor(String ext) { return TYPES.get(ext); }

    public static boolean signatureOk(String ext, byte[] h) {
        if (h == null || h.length < 12) return false;
        return switch (ext) {
            case "png" -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "webp" -> h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            case "jpg", "jpeg" -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            default -> false;
        };
    }
}
