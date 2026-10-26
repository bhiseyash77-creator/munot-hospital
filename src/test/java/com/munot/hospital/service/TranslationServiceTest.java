package com.munot.hospital.service;

import com.munot.hospital.dto.TranslateDtos.*;
import com.munot.hospital.notification.MachineTranslator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TranslationServiceTest {
    private final MachineTranslator machine = mock(MachineTranslator.class);
    private final TranslationService service = new TranslationService(machine);

    @Test
    void glossaryCoversStandardWordingWithoutCallingTheMachine() {
        var r = service.translate(new TranslateRequest(Target.MR, List.of("Take rest. Avoid heavy lifting", "5 days", "Twice daily")));
        assertTrue(r.translations().stream().allMatch(TranslatedText::complete));
        assertEquals("GLOSSARY", r.translations().get(0).method());
        assertEquals("5 दिवस", r.translations().get(1).text());
        verify(machine, never()).translate(any(), any());
    }

    @Test
    void unknownSentencesStayInEnglishAndAreFlaggedPartialWhenNoMachineIsConfigured() {
        when(machine.translate(any(), any())).thenReturn(Optional.empty());
        var r = service.translate(new TranslateRequest(Target.HI, List.of("Take rest. Apply the special compound on alternate Tuesdays")));
        assertEquals("PARTIAL", r.translations().get(0).method());
        assertFalse(r.translations().get(0).complete());
        assertEquals("आराम करें Apply the special compound on alternate Tuesdays", r.translations().get(0).text());
    }

    @SuppressWarnings("unchecked")
    @Test
    void onlyUnresolvedSentencesGoToTheMachineAndAreMappedBackToTheirPosition() {
        when(machine.configured()).thenReturn(true);
        when(machine.translate(any(), any())).thenAnswer(inv -> {
            List<String> in = inv.getArgument(0);
            return Optional.of(in.stream().map(t -> "[MR] " + t).toList());
        });
        var r = service.translate(new TranslateRequest(Target.MR, List.of("Take rest. Do the odd thing", "Weird A\nAfter food\nWeird B")));
        ArgumentCaptor<List<String>> sent = ArgumentCaptor.forClass(List.class);
        verify(machine).translate(sent.capture(), any());
        assertEquals(List.of("Do the odd thing", "Weird A", "Weird B"), sent.getValue());
        assertEquals("विश्रांती घ्या [MR] Do the odd thing", r.translations().get(0).text());
        assertEquals("[MR] Weird A\nजेवणानंतर\n[MR] Weird B", r.translations().get(1).text());
        assertEquals("MACHINE", r.translations().get(1).method());
    }
}
