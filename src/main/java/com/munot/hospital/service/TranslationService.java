package com.munot.hospital.service;

import com.munot.hospital.dto.TranslateDtos.*;
import com.munot.hospital.notification.MachineTranslator;
import com.munot.hospital.util.TranslationGlossary;
import com.munot.hospital.util.TranslationGlossary.Lang;
import org.springframework.stereotype.Service;
import java.util.*;

/** Glossary first (instant, offline), optional machine translation for what is left, English kept where nothing matched. */
@Service
public class TranslationService {
    private final MachineTranslator machine;

    public TranslationService(MachineTranslator machine) { this.machine = machine; }

    public TranslateResponse translate(TranslateRequest req) {
        Lang lang = req.target() == Target.MR ? Lang.MR : Lang.HI;
        // 1) split into sentences and look each one up in the glossary
        List<List<String>> segs = new ArrayList<>();
        List<List<String>> out = new ArrayList<>();
        List<int[]> unresolved = new ArrayList<>();      // [textIndex, segmentIndex]
        List<String> unresolvedText = new ArrayList<>();
        for (int i = 0; i < req.texts().size(); i++) {
            List<String> parts = TranslationGlossary.splitSegments(req.texts().get(i));
            List<String> done = new ArrayList<>();
            for (int j = 0; j < parts.size(); j++) {
                Optional<String> hit = TranslationGlossary.lookup(parts.get(j), lang);
                if (hit.isPresent()) done.add(hit.get());
                else { done.add(parts.get(j)); unresolved.add(new int[]{i, j}); unresolvedText.add(parts.get(j)); }
            }
            segs.add(parts);
            out.add(done);
        }
        // 2) optional machine translation for the rest
        boolean machineUsed = false;
        Set<String> stillUnresolved = new HashSet<>();
        if (!unresolved.isEmpty()) {
            Optional<List<String>> mt = machine.translate(unresolvedText, lang);
            if (mt.isPresent()) {
                machineUsed = true;
                for (int k = 0; k < unresolved.size(); k++) out.get(unresolved.get(k)[0]).set(unresolved.get(k)[1], mt.get().get(k));
            } else {
                for (int[] u : unresolved) stillUnresolved.add(u[0] + ":" + u[1]);
            }
        }
        // 3) assemble
        List<TranslatedText> result = new ArrayList<>();
        for (int i = 0; i < req.texts().size(); i++) {
            String src = req.texts().get(i);
            String sep = src != null && src.contains("\n") ? "\n" : " ";
            boolean complete = true;
            boolean usedMachine = false;
            for (int j = 0; j < out.get(i).size(); j++) {
                if (stillUnresolved.contains(i + ":" + j)) complete = false;
                for (int[] u : unresolved) if (u[0] == i && u[1] == j && machineUsed) usedMachine = true;
            }
            String method = !complete ? "PARTIAL" : usedMachine ? "MACHINE" : "GLOSSARY";
            result.add(new TranslatedText(src, String.join(sep, out.get(i)), method, complete));
        }
        return new TranslateResponse(result, machine.configured());
    }
}
