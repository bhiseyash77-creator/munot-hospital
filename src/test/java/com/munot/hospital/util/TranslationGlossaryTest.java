package com.munot.hospital.util;

import com.munot.hospital.util.TranslationGlossary.Lang;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TranslationGlossaryTest {
    private static String mr(String s) { return TranslationGlossary.lookup(s, Lang.MR).orElse(null); }
    private static String hi(String s) { return TranslationGlossary.lookup(s, Lang.HI).orElse(null); }

    @Test
    void translatesStandardFrequencyAndFoodInstructions() {
        assertEquals("दिवसातून दोनदा", mr("Twice daily"));
        assertEquals("दिन में दो बार", hi("Twice daily"));
        assertEquals("जेवणानंतर", mr("After food"));
        assertEquals("खाने के बाद", hi("after food."));
        assertEquals("रिकाम्या पोटी", mr("  EMPTY   stomach "));
    }

    @Test
    void ignoresCaseSpacingTrailingPunctuationAndLeadingFor() {
        assertEquals(mr("take rest"), mr("Take rest."));
        assertEquals(hi("5 days"), hi("for 5 days"));
        assertEquals(mr("review after 1 week"), mr("Review after one week"));
    }

    @Test
    void translatesDurationsWithNumbers() {
        assertEquals("5 दिवस", mr("5 days"));
        assertEquals("1 आठवडा", mr("1 week"));
        assertEquals("2 आठवडे", mr("2 weeks"));
        assertEquals("3 महिने", mr("3 months"));
        assertEquals("5 दिन", hi("5 days"));
        assertEquals("2 सप्ताह", hi("2 weeks"));
        assertEquals("1 महीना", hi("1 month"));
    }

    @Test
    void translatesPatternedAdvice() {
        assertEquals("दर 8 तासांनी", mr("Every 8 hours"));
        assertEquals("हर 8 घंटे में", hi("every 8 hours"));
        assertEquals("दिवसातून 3 वेळा", mr("3 times a day"));
        assertEquals("1 आठवड्यानंतर तपासणीसाठी या", mr("Review after 1 week"));
        assertEquals("2 आठवड्यांनंतर तपासणीसाठी या", mr("Review after 2 weeks"));
        assertEquals("3 दिन बाद जांच के लिए आएं", hi("Review after 3 days"));
        assertEquals("रोज 30 मिनिटे चाला", mr("Walk for 30 minutes daily"));
    }

    @Test
    void doseSchedulesLikeOneZeroOneStayUnchanged() {
        assertEquals("1-0-1", mr("1-0-1"));
        assertEquals("1-1-1", hi("1-1-1"));
    }

    @Test
    void unknownTextIsNotGuessed() {
        assertNull(mr("Apply the special compound twice on alternate Tuesdays"));
        assertNull(hi(""));
    }

    @Test
    void splitsAdviceIntoSentencesAndLines() {
        var parts = TranslationGlossary.splitSegments("Take rest. Avoid heavy lifting; walk daily\nDrink plenty of water");
        assertEquals(4, parts.size());
        assertEquals("Take rest.", parts.get(0));
        assertEquals("Drink plenty of water", parts.get(3));
        assertNotNull(mr(parts.get(1)));
    }
}
