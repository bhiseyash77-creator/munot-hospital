package com.munot.hospital.notification;

import com.munot.hospital.config.AppProperties;
import com.munot.hospital.util.TranslationGlossary.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Optional machine translation for sentences the built-in glossary does not know.
 * Providers: "google" (Cloud Translation v2 REST) or "libretranslate". Configured by TRANSLATE_PROVIDER / _API_URL / _API_KEY.
 * Only the advice sentences are sent - no patient names or identifiers. The doctor always reviews the result before saving.
 */
@Component
public class MachineTranslator {
    private static final Logger log = LoggerFactory.getLogger(MachineTranslator.class);
    private static final String GOOGLE_DEFAULT = "https://translation.googleapis.com/language/translate/v2";

    private final AppProperties props;
    private final RestClient http = RestClient.create();

    public MachineTranslator(AppProperties props) { this.props = props; }

    private String provider() { return props.translate() == null || props.translate().provider() == null ? "" : props.translate().provider().trim().toLowerCase(); }

    public boolean configured() {
        AppProperties.Translate t = props.translate();
        if (t == null) return false;
        return switch (provider()) {
            case "google" -> t.apiKey() != null && !t.apiKey().isBlank();
            case "libretranslate" -> t.url() != null && !t.url().isBlank();
            default -> false;
        };
    }

    /** Returns one translation per input, or empty if the provider is not configured or fails (caller falls back gracefully). */
    @SuppressWarnings("unchecked")
    public Optional<List<String>> translate(List<String> texts, Lang lang) {
        if (!configured() || texts.isEmpty()) return Optional.empty();
        String target = lang == Lang.MR ? "mr" : "hi";
        AppProperties.Translate t = props.translate();
        try {
            if (provider().equals("google")) {
                String url = t.url() == null || t.url().isBlank() ? GOOGLE_DEFAULT : t.url();
                Map<String, Object> body = http.post().uri(url).header("X-goog-api-key", t.apiKey()).contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("q", texts, "source", "en", "target", target, "format", "text"))
                        .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {});
                List<Map<String, Object>> tr = (List<Map<String, Object>>) ((Map<String, Object>) body.get("data")).get("translations");
                List<String> out = new ArrayList<>();
                for (Map<String, Object> m : tr) out.add(String.valueOf(m.get("translatedText")));
                return out.size() == texts.size() ? Optional.of(out) : Optional.empty();
            }
            Map<String, Object> req = new HashMap<>(Map.of("q", texts, "source", "en", "target", target, "format", "text"));
            if (t.apiKey() != null && !t.apiKey().isBlank()) req.put("api_key", t.apiKey());
            Map<String, Object> body = http.post().uri(t.url()).contentType(MediaType.APPLICATION_JSON).body(req)
                    .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {});
            Object tt = body.get("translatedText");
            List<String> out = new ArrayList<>();
            if (tt instanceof List<?> l) for (Object o : l) out.add(String.valueOf(o));
            else if (tt != null && texts.size() == 1) out.add(String.valueOf(tt));
            return out.size() == texts.size() ? Optional.of(out) : Optional.empty();
        } catch (Exception e) {
            log.warn("Machine translation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
