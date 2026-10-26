package com.munot.hospital.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImageRulesTest {
    private static byte[] bytes(int... v) { byte[] b = new byte[12]; for (int i = 0; i < v.length; i++) b[i] = (byte) v[i]; return b; }

    @Test
    void allowsOnlyImageExtensions() {
        assertEquals("image/jpeg", ImageRules.contentTypeFor(ImageRules.extension("Clinic Front.JPG")));
        assertEquals("image/png", ImageRules.contentTypeFor("png"));
        assertEquals("image/webp", ImageRules.contentTypeFor("webp"));
        assertNull(ImageRules.contentTypeFor("svg"));    // SVG can carry scripts: not allowed
        assertNull(ImageRules.contentTypeFor("exe"));
        assertEquals("", ImageRules.extension("noextension"));
    }

    @Test
    void rejectsFilesWhoseContentDoesNotMatchTheirExtension() {
        assertTrue(ImageRules.signatureOk("jpg", bytes(0xFF, 0xD8, 0xFF, 0xE0)));
        assertTrue(ImageRules.signatureOk("png", bytes(0x89, 'P', 'N', 'G')));
        byte[] webp = bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P');
        assertTrue(ImageRules.signatureOk("webp", webp));
        assertFalse(ImageRules.signatureOk("png", bytes(0xFF, 0xD8, 0xFF, 0xE0)));      // a JPEG renamed to .png
        assertFalse(ImageRules.signatureOk("jpg", bytes('M', 'Z', 0x90, 0)));          // a Windows program renamed to .jpg
        assertFalse(ImageRules.signatureOk("jpg", new byte[3]));
        assertFalse(ImageRules.signatureOk("gif", bytes('G', 'I', 'F', '8')));
    }
}
