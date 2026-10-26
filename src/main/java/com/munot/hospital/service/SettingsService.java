package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.entity.HospitalSetting;
import com.munot.hospital.util.MedicalDocumentRules;
import com.munot.hospital.repository.HospitalSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {
    /** Keys exposed on the public website. */
    public static final Set<String> PUBLIC_KEYS = Set.of("hospital_name", "tagline", "address", "phone", "emergency_phone",
            "email", "opening_hours", "map_embed_url", "about_text");

    private final HospitalSettingRepository repo;
    private final AuditService audit;

    public String get(String key, String fallback) {
        return repo.findBySettingKey(key).map(HospitalSetting::getSettingValue).filter(v -> v != null && !v.isBlank()).orElse(fallback);
    }

    public int getInt(String key, int fallback) {
        try { return Integer.parseInt(get(key, String.valueOf(fallback)).trim()); } catch (NumberFormatException e) { return fallback; }
    }

    public Map<String, String> all() {
        Map<String, String> m = new TreeMap<>();
        repo.findAll().forEach(s -> m.put(s.getSettingKey(), s.getSettingValue()));
        return m;
    }

    public Map<String, String> publicSettings() {
        Map<String, String> m = new TreeMap<>();
        all().forEach((k, v) -> { if (PUBLIC_KEYS.contains(k)) m.put(k, v); });
        return m;
    }

    @Transactional
    public Map<String, String> update(Map<String, String> values) {
        values.forEach((k, v) -> {
            if (k == null || !k.matches("^[a-z0-9_]{1,80}$")) return;
            if (k.startsWith("print_margin_")) {
                int mm;
                try { mm = Integer.parseInt(v == null ? "" : v.trim()); } catch (NumberFormatException e) { throw ApiException.badRequest("Margins must be whole numbers of millimetres"); }
                if (!MedicalDocumentRules.validMargin(k, mm)) throw ApiException.badRequest("Margin out of range for " + k);
                v = String.valueOf(mm);
            }
            HospitalSetting s = repo.findBySettingKey(k).orElseGet(() -> { HospitalSetting n = new HospitalSetting(); n.setSettingKey(k); return n; });
            s.setSettingValue(v == null ? "" : v);
            repo.save(s);
        });
        audit.log("SETTINGS_UPDATED", "HospitalSetting", null, "keys=" + values.keySet());
        return all();
    }
}
