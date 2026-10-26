package com.munot.hospital.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in English -> Marathi / Hindi glossary for standard prescription wording (frequency, timing, food relation,
 * durations, common orthopaedic advice and test names). Pure Java: works offline, is deterministic and unit-tested.
 * Anything not covered here is left to the optional machine-translation provider or typed by the doctor.
 */
public final class TranslationGlossary {
    private TranslationGlossary() {}

    public enum Lang { MR, HI }

    private static final Map<String, String[]> PHRASES = new HashMap<>();
    private static final Map<String, String> NUMBER_WORDS = Map.ofEntries(
            Map.entry("one", "1"), Map.entry("two", "2"), Map.entry("three", "3"), Map.entry("four", "4"), Map.entry("five", "5"),
            Map.entry("six", "6"), Map.entry("seven", "7"), Map.entry("ten", "10"), Map.entry("fifteen", "15"), Map.entry("thirty", "30"));

    private static void p(String en, String mr, String hi) { PHRASES.put(normalize(en), new String[]{mr, hi}); }

    private static void p(String[] enVariants, String mr, String hi) { for (String e : enVariants) p(e, mr, hi); }

    static {
        // --- how often / when
        p("once daily", "दिवसातून एकदा", "दिन में एक बार");
        p("twice daily", "दिवसातून दोनदा", "दिन में दो बार");
        p("three times daily", "दिवसातून तीन वेळा", "दिन में तीन बार");
        p("four times daily", "दिवसातून चार वेळा", "दिन में चार बार");
        p("at bedtime", "झोपण्यापूर्वी", "सोने से पहले");
        p(new String[]{"sos (if needed)", "sos", "if needed", "when needed", "only if needed"}, "गरज असल्यास", "ज़रूरत पड़ने पर");
        p("morning", "सकाळी", "सुबह");
        p("evening", "संध्याकाळी", "शाम को");
        p("night", "रात्री", "रात को");
        p("morning and night", "सकाळी आणि रात्री", "सुबह और रात को");
        p("every morning", "रोज सकाळी", "रोज़ सुबह");
        p("every night", "रोज रात्री", "रोज़ रात को");
        p("once a week", "आठवड्यातून एकदा", "सप्ताह में एक बार");
        p("daily", "रोज", "रोज़");
        // --- relation to food / how to take
        p(new String[]{"before food", "before meals"}, "जेवणापूर्वी", "खाने से पहले");
        p(new String[]{"after food", "after meals"}, "जेवणानंतर", "खाने के बाद");
        p(new String[]{"with food", "with meals"}, "जेवणासोबत", "खाने के साथ");
        p("empty stomach", "रिकाम्या पोटी", "खाली पेट");
        p("with water", "पाण्यासोबत", "पानी के साथ");
        p("with warm water", "कोमट पाण्यासोबत", "गुनगुने पानी के साथ");
        p("with milk", "दुधासोबत", "दूध के साथ");
        p("take with plenty of water", "भरपूर पाण्यासोबत घ्या", "खूब पानी के साथ लें");
        p("dissolve in water", "पाण्यात विरघळवून घ्या", "पानी में घोलकर लें");
        p("swallow whole", "संपूर्ण गिळा", "पूरा निगलें");
        p(new String[]{"apply locally", "apply on the affected area", "apply on the painful area"}, "दुखऱ्या जागी लावा", "दर्द वाली जगह पर लगाएं");
        p("apply gently", "हळुवारपणे लावा", "धीरे से लगाएं");
        p("apply twice daily", "दिवसातून दोनदा लावा", "दिन में दो बार लगाएं");
        p("apply three times daily", "दिवसातून तीन वेळा लावा", "दिन में तीन बार लगाएं");
        p("do not take painkillers on an empty stomach", "वेदनाशामक औषधे रिकाम्या पोटी घेऊ नका", "दर्द की दवाइयां खाली पेट न लें");
        // --- medicines
        p(new String[]{"continue the medicines as prescribed", "continue medicines as prescribed"}, "औषधे सांगितल्याप्रमाणे चालू ठेवा", "दवाइयां बताए अनुसार जारी रखें");
        p("take medicines regularly", "औषधे नियमित घ्या", "दवाइयां नियमित रूप से लें");
        p("do not stop medicines without asking the doctor", "डॉक्टरांना विचारल्याशिवाय औषधे बंद करू नका", "डॉक्टर से पूछे बिना दवाइयां बंद न करें");
        // --- advice (orthopaedic)
        p(new String[]{"take rest", "rest"}, "विश्रांती घ्या", "आराम करें");
        p("complete bed rest", "पूर्ण विश्रांती घ्या", "पूरा आराम करें");
        p("avoid heavy lifting", "जड वस्तू उचलू नका", "भारी वस्तुएं न उठाएं");
        p("avoid bending forward", "पुढे वाकणे टाळा", "आगे झुकने से बचें");
        p("avoid squatting", "उकिडवे बसणे टाळा", "उकड़ूं बैठने से बचें");
        p("avoid sitting on the floor", "जमिनीवर बसणे टाळा", "ज़मीन पर बैठने से बचें");
        p("avoid stairs", "जिने चढणे टाळा", "सीढ़ियां चढ़ने से बचें");
        p("avoid sitting for long periods", "बराच वेळ बसणे टाळा", "लंबे समय तक बैठने से बचें");
        p("avoid sudden movements", "अचानक हालचाली टाळा", "अचानक हरकतों से बचें");
        p("use a western toilet", "कमोडचा वापर करा", "वेस्टर्न टॉयलेट का उपयोग करें");
        p(new String[]{"hot fomentation", "hot compress"}, "गरम शेक घ्या", "गरम सेंक करें");
        p("hot fomentation twice a day", "दिवसातून दोनदा गरम शेक घ्या", "दिन में दो बार गरम सेंक करें");
        p(new String[]{"apply ice pack", "apply an ice pack"}, "बर्फाचा शेक द्या", "बर्फ की सिकाई करें");
        p(new String[]{"do not put weight on the leg", "do not bear weight on the leg", "do not put weight on the injured leg"}, "पायावर वजन देऊ नका", "पैर पर वजन न डालें");
        p("partial weight bearing as advised", "सांगितल्याप्रमाणे थोडे वजन द्या", "बताए अनुसार थोड़ा वजन डालें");
        p(new String[]{"keep the cast dry", "keep the plaster dry"}, "प्लास्टर कोरडे ठेवा", "प्लास्टर को सूखा रखें");
        p("keep the leg elevated", "पाय उंच ठेवा", "पैर को ऊंचा रखें");
        p(new String[]{"keep the arm elevated", "keep the hand elevated"}, "हात उंच ठेवा", "हाथ को ऊंचा रखें");
        p(new String[]{"do physiotherapy exercises as advised", "do the physiotherapy exercises as advised"}, "सांगितल्याप्रमाणे फिजिओथेरपीचे व्यायाम करा", "बताए अनुसार फिजियोथेरेपी के व्यायाम करें");
        p("do the exercises as advised", "सांगितल्याप्रमाणे व्यायाम करा", "बताए अनुसार व्यायाम करें");
        p("walk daily", "रोज चाला", "रोज़ टहलें");
        p(new String[]{"drink plenty of water", "drink plenty of fluids"}, "भरपूर पाणी प्या", "खूब पानी पिएं");
        p("eat a healthy diet", "संतुलित आहार घ्या", "संतुलित आहार लें");
        p("avoid smoking", "धूम्रपान टाळा", "धूम्रपान से बचें");
        p("avoid alcohol", "मद्यपान टाळा", "शराब से बचें");
        p("reduce weight", "वजन कमी करा", "वजन कम करें");
        p("do not apply pressure on the injured area", "दुखापतीच्या जागी दाब देऊ नका", "चोट वाली जगह पर दबाव न डालें");
        p("do not massage the area", "त्या जागी मालिश करू नका", "उस जगह मालिश न करें");
        p(new String[]{"use crutches", "walk with crutches"}, "कुबड्या वापरा", "बैसाखी का उपयोग करें");
        p("use a walker as advised", "सांगितल्याप्रमाणे वॉकर वापरा", "बताए अनुसार वॉकर का उपयोग करें");
        p("wear the belt as advised", "सांगितल्याप्रमाणे बेल्ट वापरा", "बताए अनुसार बेल्ट पहनें");
        p(new String[]{"come immediately if pain or swelling increases", "come immediately if pain and swelling increase"}, "वेदना किंवा सूज वाढल्यास ताबडतोब या", "दर्द या सूजन बढ़ने पर तुरंत आएं");
        p("come immediately if pain increases", "वेदना वाढल्यास ताबडतोब या", "दर्द बढ़ने पर तुरंत आएं");
        p("contact the hospital immediately if you develop fever", "ताप आल्यास ताबडतोब रुग्णालयाशी संपर्क साधा", "बुखार आने पर तुरंत अस्पताल से संपर्क करें");
        p("bring all reports on the next visit", "पुढील भेटीत सर्व रिपोर्ट सोबत आणा", "अगली बार सभी रिपोर्ट साथ लाएं");
        p("bring previous reports", "जुने रिपोर्ट सोबत आणा", "पुरानी रिपोर्ट साथ लाएं");
        // --- tests
        p("x-ray", "एक्स-रे", "एक्स-रे");
        p("mri", "एमआरआय", "एमआरआई");
        p("ct scan", "सीटी स्कॅन", "सीटी स्कैन");
        p(new String[]{"blood test", "blood tests"}, "रक्त तपासणी", "खून की जांच");
        p("urine test", "लघवी तपासणी", "पेशाब की जांच");
        p("vitamin d test", "व्हिटॅमिन डी तपासणी", "विटामिन डी की जांच");
        p("bone density test", "हाडांची घनता तपासणी", "हड्डियों की घनता की जांच");
        p("ultrasound", "सोनोग्राफी", "सोनोग्राफी");
    }

    private static final Pattern DURATION = Pattern.compile("^(\\d+) (day|days|week|weeks|month|months)$");
    private static final Pattern EVERY_HOURS = Pattern.compile("^every (\\d+) hours?$");
    private static final Pattern TIMES_A_DAY = Pattern.compile("^(\\d+) times? (?:a|per) day$");
    private static final Pattern REVIEW = Pattern.compile("^review after (\\d+) (day|days|week|weeks)$");
    private static final Pattern WALK = Pattern.compile("^walk for (\\d+) minutes daily$");
    private static final Pattern ICE = Pattern.compile("^apply (?:an? )?ice(?: pack)? for (\\d+) minutes$");
    private static final Pattern CONTINUE = Pattern.compile("^continue for (\\d+) days$");
    private static final Pattern NEUTRAL = Pattern.compile("^[0-9+\\-/ ]+$");

    /** Lower-case, collapse spaces, drop trailing punctuation and a leading "for", turn number words into digits. */
    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        t = t.replaceAll("[.;,!]+$", "").trim();
        if (t.startsWith("for ")) t = t.substring(4);
        StringBuilder out = new StringBuilder();
        for (String w : t.split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(NUMBER_WORDS.getOrDefault(w, w));
        }
        return out.toString();
    }

    /** Splits free text into sentences / lines so each can be matched on its own. */
    public static List<String> splitSegments(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String part : text.split("(?<=[.;!?])\\s+|\\r?\\n+")) if (!part.isBlank()) out.add(part.trim());
        return out;
    }

    private static String unit(String u, int n, Lang l) {
        boolean one = n == 1;
        return switch (u.replaceAll("s$", "")) {
            case "day" -> l == Lang.MR ? "दिवस" : "दिन";
            case "week" -> l == Lang.MR ? (one ? "आठवडा" : "आठवडे") : "सप्ताह";
            default -> l == Lang.MR ? (one ? "महिना" : "महिने") : (one ? "महीना" : "महीने");
        };
    }

    public static Optional<String> lookup(String text, Lang lang) {
        String n = normalize(text);
        if (n.isEmpty()) return Optional.empty();
        String[] hit = PHRASES.get(n);
        if (hit != null) return Optional.of(lang == Lang.MR ? hit[0] : hit[1]);
        if (NEUTRAL.matcher(n).matches()) return Optional.of(text.trim());
        boolean mr = lang == Lang.MR;
        Matcher m;
        if ((m = DURATION.matcher(n)).matches()) { int c = Integer.parseInt(m.group(1)); return Optional.of(c + " " + unit(m.group(2), c, lang)); }
        if ((m = EVERY_HOURS.matcher(n)).matches()) { int c = Integer.parseInt(m.group(1)); return Optional.of(mr ? "दर " + c + " तासांनी" : "हर " + c + " घंटे में"); }
        if ((m = TIMES_A_DAY.matcher(n)).matches()) { int c = Integer.parseInt(m.group(1)); return Optional.of(mr ? "दिवसातून " + c + " वेळा" : "दिन में " + c + " बार"); }
        if ((m = REVIEW.matcher(n)).matches()) {
            int c = Integer.parseInt(m.group(1));
            String u = m.group(2).replaceAll("s$", "");
            if (mr) {
                String after = u.equals("day") ? (c == 1 ? "दिवसानंतर" : "दिवसांनंतर") : (c == 1 ? "आठवड्यानंतर" : "आठवड्यांनंतर");
                return Optional.of(c + " " + after + " तपासणीसाठी या");
            }
            return Optional.of(c + " " + (u.equals("day") ? "दिन" : "सप्ताह") + " बाद जांच के लिए आएं");
        }
        if ((m = WALK.matcher(n)).matches()) { int c = Integer.parseInt(m.group(1)); return Optional.of(mr ? "रोज " + c + " मिनिटे चाला" : "रोज़ " + c + " मिनट टहलें"); }
        if ((m = ICE.matcher(n)).matches()) { int c = Integer.parseInt(m.group(1)); return Optional.of(mr ? "बर्फाचा शेक " + c + " मिनिटे द्या" : c + " मिनट बर्फ की सिकाई करें"); }
        if ((m = CONTINUE.matcher(n)).matches()) { int c = Integer.parseInt(m.group(1)); return Optional.of(mr ? c + " दिवस चालू ठेवा" : c + " दिन जारी रखें"); }
        return Optional.empty();
    }
}
