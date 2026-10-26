package com.munot.hospital.util;

import com.munot.hospital.entity.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatRulesTest {
    @Test
    void onlyDoctorsAndReceptionistsMayChat() {
        assertTrue(ChatRules.canUse(Role.DOCTOR));
        assertTrue(ChatRules.canUse(Role.RECEPTIONIST));
        assertFalse(ChatRules.canUse(Role.PATIENT));
        assertFalse(ChatRules.canUse(Role.ADMIN));
    }

    @Test
    void trimsAndCollapsesBlankLines() {
        assertEquals("Patient is waiting", ChatRules.normalize("  Patient is waiting \n"));
        assertEquals("a\n\nb", ChatRules.normalize("a\n\n\n\n\nb"));
        assertEquals("a\nb", ChatRules.normalize("a\r\nb"));
    }

    @Test
    void rejectsEmptyAndOverlongMessages() {
        assertThrows(IllegalArgumentException.class, () -> ChatRules.normalize("   \n  "));
        assertThrows(IllegalArgumentException.class, () -> ChatRules.normalize(null));
        assertEquals(1000, ChatRules.normalize("x".repeat(1000)).length());
        assertThrows(IllegalArgumentException.class, () -> ChatRules.normalize("x".repeat(1001)));
    }

    @Test
    void keepsMarathiAndHindiText() {
        assertEquals("रुग्ण आले आहेत", ChatRules.normalize(" रुग्ण आले आहेत "));
    }
}
